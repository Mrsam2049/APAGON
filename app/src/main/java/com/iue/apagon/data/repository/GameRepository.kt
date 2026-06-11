package com.iue.apagon.data.repository

import android.content.Context
import com.iue.apagon.data.local.AppDatabase
import com.iue.apagon.data.local.dao.EnergiaDao
import com.iue.apagon.data.local.dao.PartidaDao
import com.iue.apagon.data.local.entity.EnergiaDiariaEntity
import com.iue.apagon.data.local.entity.PartidaEntity
import com.iue.apagon.data.remote.NetworkModule
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
    private val energiaDao: EnergiaDao
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
                        energiaDao = db.energiaDao()
                    ).also { INSTANCE = it }
                }
            }
    }
}
