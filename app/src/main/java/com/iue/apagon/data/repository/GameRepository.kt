package com.iue.apagon.data.repository

import android.content.Context
import com.iue.apagon.data.local.AppDatabase
import com.iue.apagon.data.local.dao.EnergiaDao
import com.iue.apagon.data.local.dao.LogroDao
import com.iue.apagon.data.local.dao.PartidaDao
import com.iue.apagon.data.local.dao.PerfilDao
import com.iue.apagon.data.local.entity.EnergiaDiariaEntity
import com.iue.apagon.data.local.entity.LogroEntity
import com.iue.apagon.data.local.entity.PartidaEntity
import com.iue.apagon.data.local.entity.PerfilEntity
import com.iue.apagon.data.remote.NetworkModule
import com.iue.apagon.domain.engine.Logro
import com.iue.apagon.domain.engine.Unlockable
import com.iue.apagon.domain.engine.UnlockKind
import com.iue.apagon.data.remote.SimemApiService
import com.iue.apagon.data.remote.XmApiService
import com.iue.apagon.data.remote.dto.XmRequest
import com.iue.apagon.domain.model.GameMode
import com.iue.apagon.domain.model.GameState
import com.iue.apagon.domain.model.Municipio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Punto único de acceso a datos (red + Room). Singleton manual (sin Hilt).
 */
class GameRepository private constructor(
    private val xmApi: XmApiService,
    private val simemApi: SimemApiService,
    private val partidaDao: PartidaDao,
    private val energiaDao: EnergiaDao,
    private val perfilDao: PerfilDao,
    private val logroDao: LogroDao
) {

    /**
     * Obtiene los datos de energía con estrategia de fallback:
     *   1. SiMEM (dataset E17D25).
     *   2. Si falla, SINERGOX (VoluUtilDiarEner).
     *   3. Si ambas fallan, el registro más reciente cacheado en Room.
     * Las llamadas exitosas se cachean en Room antes de devolverse.
     */
    suspend fun fetchEnergyData(): Result<EnergiaDiariaEntity> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()

        // 1) SiMEM — dataset E17D25 (Generación Real)
        runCatching {
            val response = simemApi.getGeneracionReal(
                datasetId = DATASET_GENERACION_REAL,
                startDate = isoDaysAgo(LOOKBACK_DAYS),
                endDate = isoDaysAgo(0)
            )
            val (fecha, valor) = response.latestValue()
                ?: error("SiMEM no devolvió registros utilizables")
            EnergiaDiariaEntity(
                fecha = fecha.take(10),
                fuente = SOURCE_SIMEM,
                valorMw = valor,
                fetchedAtEpoch = now
            )
        }.onSuccess { entity ->
            energiaDao.upsertAll(listOf(entity))
            return@withContext Result.success(entity)
        }

        // 2) SINERGOX — VoluUtilDiarEner
        runCatching {
            val response = xmApi.getDailyMetric(
                XmRequest(
                    metricId = METRIC_VOLUMEN_UTIL,
                    startDate = isoDaysAgo(LOOKBACK_DAYS),
                    endDate = isoDaysAgo(0),
                    entity = ENTITY_SISTEMA
                )
            )
            val (fecha, valor) = response.latestValue()
                ?: error("SINERGOX no devolvió registros utilizables")
            EnergiaDiariaEntity(
                fecha = fecha.take(10),
                fuente = SOURCE_SINERGOX,
                valorMw = valor,
                fetchedAtEpoch = now
            )
        }.onSuccess { entity ->
            energiaDao.upsertAll(listOf(entity))
            return@withContext Result.success(entity)
        }

        // 3) Caché local (Room)
        val cached = energiaDao.masReciente()
        if (cached != null) {
            Result.success(cached)
        } else {
            Result.failure(
                IllegalStateException(
                    "No se pudo obtener energía: SiMEM y SINERGOX fallaron y no hay caché en Room."
                )
            )
        }
    }

    /**
     * Convierte el GameState terminado a PartidaEntity y lo persiste. Devuelve el id generado.
     */
    suspend fun savePartida(
        state: GameState,
        mode: GameMode,
        municipio: Municipio
    ): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val ind = state.indicators
        val entity = PartidaEntity(
            modo = mode.name,
            municipio = municipio.name,
            fechaInicioEpoch = now,
            fechaFinEpoch = now,
            nochesCompletadas = state.day,
            victoria = state.victory,
            razonFin = state.gameOverReason?.name,
            coberturaFinal = ind.coverage.toDouble(),
            presupuestoFinal = ind.budget.toDouble(),
            bienestarFinal = ind.social.toDouble(),
            ambientalFinal = ind.environmental.toDouble(),
            puntajeFinal = computeScore(state)
        )
        partidaDao.insertPartida(entity)
    }

    /** Historial de partidas observable, directamente desde el DAO. */
    fun getHistorial(): Flow<List<PartidaEntity>> = partidaDao.observarHistorial()

    // ─────────────────────────── Perfil / progresión ───────────────────────────

    /** Perfil observable (Flow). */
    fun observarPerfil(): Flow<PerfilEntity?> = perfilDao.get()

    /** Devuelve el perfil, creando la fila por defecto si hiciera falta. */
    suspend fun perfil(): PerfilEntity = withContext(Dispatchers.IO) {
        perfilDao.insertDefault(PerfilEntity())
        perfilDao.getOnce() ?: PerfilEntity()
    }

    /** Acredita (o descuenta) Vatios al perfil. */
    suspend fun addVatios(cantidad: Int) = withContext(Dispatchers.IO) {
        perfilDao.addVatios(cantidad)
    }

    /**
     * Intenta desbloquear un ítem: valida saldo, lo agrega al CSV correspondiente y descuenta
     * los Vatios. Devuelve el perfil actualizado, o un fallo (saldo insuficiente / ya comprado).
     */
    suspend fun desbloquear(item: Unlockable): Result<PerfilEntity> = withContext(Dispatchers.IO) {
        val p = perfil()
        if (p.vatiosTotales < item.costo) {
            return@withContext Result.failure(IllegalStateException("Vatios insuficientes"))
        }
        val cartas = csv(p.cartasDesbloqueadas)
        val municipios = csv(p.municipiosDesbloqueados)
        val mejoras = csv(p.mejorasCompradas)

        val yaTiene = when (item.kind) {
            UnlockKind.CARTA -> item.id in cartas
            UnlockKind.MUNICIPIO -> item.id in municipios
            UnlockKind.MEJORA -> item.id in mejoras
        }
        if (yaTiene) return@withContext Result.failure(IllegalStateException("Ya desbloqueado"))

        val actualizado = when (item.kind) {
            UnlockKind.CARTA -> p.copy(cartasDesbloqueadas = join(cartas + item.id))
            UnlockKind.MUNICIPIO -> p.copy(municipiosDesbloqueados = join(municipios + item.id))
            UnlockKind.MEJORA -> p.copy(mejorasCompradas = join(mejoras + item.id))
        }.let { it.copy(vatiosTotales = it.vatiosTotales - item.costo) }

        perfilDao.update(actualizado)
        Result.success(actualizado)
    }

    private fun csv(value: String): Set<String> =
        value.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()

    private fun join(set: Set<String>): String = set.joinToString(",")

    // ──────────────────────────────── Logros ───────────────────────────────────

    /** Logros observables (incluye los bloqueados). */
    fun observarLogros(): Flow<List<LogroEntity>> = logroDao.observe()

    /** Asegura que existan las filas de todos los logros (bloqueados por defecto). */
    suspend fun ensureLogros() = withContext(Dispatchers.IO) {
        logroDao.insertIgnore(Logro.entries.map { LogroEntity(it.name, desbloqueado = false) })
    }

    /**
     * Marca como desbloqueados los logros [satisfechos] que aún no lo estaban.
     * Devuelve solo los NUEVOS (para acreditar Vatios y notificar a la UI una sola vez).
     */
    suspend fun desbloquearLogros(satisfechos: Set<Logro>): List<Logro> = withContext(Dispatchers.IO) {
        ensureLogros()
        val yaDesbloqueados = logroDao.getDesbloqueadosIds().toSet()
        val nuevos = satisfechos.filter { it.name !in yaDesbloqueados }
        nuevos.forEach { logroDao.marcarDesbloqueado(it.name) }
        nuevos
    }

    /** Puntaje sencillo a partir de indicadores y noches superadas. Ajustable. */
    private fun computeScore(state: GameState): Int {
        val ind = state.indicators
        val base = ind.coverage + ind.social + ind.environmental + ind.budget
        val nightsBonus = state.day * 20
        val victoryBonus = if (state.victory) 150 else 0
        return base + nightsBonus + victoryBonus
    }

    private fun isoDaysAgo(days: Int): String {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -days) }
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    companion object {
        private const val DATASET_GENERACION_REAL = "E17D25"
        private const val METRIC_VOLUMEN_UTIL = "VoluUtilDiarEner"
        private const val ENTITY_SISTEMA = "Sistema"
        private const val SOURCE_SIMEM = "SIMEM"
        private const val SOURCE_SINERGOX = "SINERGOX"
        private const val LOOKBACK_DAYS = 7

        @Volatile private var INSTANCE: GameRepository? = null

        fun get(context: Context): GameRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    val db = AppDatabase.get(context)
                    GameRepository(
                        xmApi = NetworkModule.xmApi,
                        simemApi = NetworkModule.simemApi,
                        partidaDao = db.partidaDao(),
                        energiaDao = db.energiaDao(),
                        perfilDao = db.perfilDao(),
                        logroDao = db.logroDao()
                    ).also { INSTANCE = it }
                }
            }
    }
}
