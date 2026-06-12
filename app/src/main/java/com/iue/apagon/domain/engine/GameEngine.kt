package com.iue.apagon.domain.engine

import com.iue.apagon.domain.model.Card
import com.iue.apagon.domain.model.CardType
import com.iue.apagon.domain.model.District
import com.iue.apagon.domain.model.DistrictTrait
import com.iue.apagon.domain.model.EnergySource
import com.iue.apagon.domain.model.GameMode
import com.iue.apagon.domain.model.GameOverReason
import com.iue.apagon.domain.model.GameState
import kotlin.math.roundToInt

/**
 * Núcleo de reglas del juego. Clase pura: sin estado mutable propio, sin Android, sin Room.
 * Todos los métodos reciben un [GameState] (u otros valores) y devuelven nuevas instancias;
 * nunca modifican los objetos recibidos.
 */
class GameEngine {

    // ───────────────────────────── Consultas puras ─────────────────────────────

    /** Intensidad del fenómeno El Niño (solo informativa en supervivencia). */
    fun elNinoIntensity(day: Int): Float = 1f + (day - 1) * 0.18f

    /**
     * Energía base de la noche según el escenario, ya con la degradación de El Niño.
     * - Campaña: el valor del escenario tal cual.
     * - Supervivencia: max(38, energyBaseMw - (day - 1) * 4).
     */
    fun baseEnergy(state: GameState): Int {
        val escenario = when (state.mode) {
            GameMode.CAMPANA -> state.scenario.energyBaseMw
            GameMode.SUPERVIVENCIA ->
                maxOf(38, state.scenario.energyBaseMw - (state.day - 1) * 4)
        }
        // Mejora "energia_base": +MW disponibles cada noche.
        return escenario + state.bonusEnergiaBase
    }

    /** Energía disponible total = energía base + bonus aportado por cartas. */
    fun availableEnergy(state: GameState): Int = baseEnergy(state) + state.bonusEnergy

    /**
     * Ingresos del día. Se suman al inicio de la noche (ver [startNight]).
     * - Campaña: +10 fijos.
     * - Supervivencia: max(6, 12 - floor(day / 2)).
     */
    fun dailyIncome(state: GameState): Int = when (state.mode) {
        GameMode.CAMPANA -> 10
        GameMode.SUPERVIVENCIA -> maxOf(6, 12 - state.day / 2)
    }

    /**
     * Cobertura = poweredDemand / totalDemand * 100, redondeada.
     * Solo cuentan los distritos en juego (lost == false).
     */
    fun coverage(districts: List<District>): Int {
        val active = districts.filter { !it.lost }
        val total = active.sumOf { it.demand }
        if (total <= 0) return 0
        val powered = active.filter { it.on }.sumOf { it.demand }
        return (powered.toDouble() / total * 100).roundToInt()
    }

    // ─────────────────────────── Inicio de la noche ────────────────────────────

    /**
     * Prepara el estado para la noche actual: suma los ingresos del día (al inicio, no al
     * terminar), reinicia los flags por-noche y enciende los distritos en juego.
     * No avanza el día ni reparte mano (eso lo decide la capa superior).
     */
    fun startNight(state: GameState): GameState {
        val mods = state.scenario.demandModifiers
        val resetDistricts = state.districts.map { d ->
            if (d.lost) d
            else d.copy(on = true, demand = (d.baseDemand * (mods[d.id] ?: 1f)).roundToInt())
        }
        val indicators = state.indicators.copy(
            budget = state.indicators.budget + dailyIncome(state),
            coverage = coverage(resetDistricts)
        )
        return state.copy(
            districts = resetDistricts,
            bonusEnergy = 0,
            campaignActive = false,
            // Mejora "cuadrilla_extra": +1 jugada por noche.
            actionPoints = 2 + state.bonusAccionesPorNoche,
            indicators = indicators
        )
    }

    // ──────────────────────────────── Jugar carta ──────────────────────────────

    /**
     * Aplica el efecto de una carta. Los efectos ocurren al jugarse (no al terminar la noche).
     * Descuenta una jugada y retira la carta de la mano.
     */
    fun applyCardEffect(state: GameState, card: Card): GameState {
        var districts = state.districts
        var bonusEnergy = state.bonusEnergy
        var amb = state.indicators.environmental
        var campaignActive = state.campaignActive
        var extraBudget = 0

        when (card.type) {
            CardType.ENERGIA -> {
                bonusEnergy += card.mw
                amb = (amb + ambDelta(card.source)).coerceIn(0, 100)
            }
            CardType.RACIONAMIENTO -> {
                val activeCount = districts.count { !it.lost }.coerceAtLeast(1)
                val reduction = (20.0 / activeCount).roundToInt()
                // "mínimo 8": la demanda resultante no baja de 8.
                districts = districts.map { d ->
                    if (d.lost) d else d.copy(demand = (d.demand - reduction).coerceAtLeast(8))
                }
            }
            CardType.EFICIENCIA -> {
                // Reduce un 30% la demanda de cada distrito (mínimo 8).
                districts = districts.map { d ->
                    if (d.lost) d else d.copy(demand = (d.demand * 0.7).roundToInt().coerceAtLeast(8))
                }
            }
            CardType.SUBSIDIO -> extraBudget += card.mw  // mw guarda los millones del subsidio
            CardType.CAMPANA_CIUDADANA -> campaignActive = true
        }

        return state.copy(
            districts = districts,
            hand = state.hand - card,
            actionPoints = (state.actionPoints - 1).coerceAtLeast(0),
            bonusEnergy = bonusEnergy,
            campaignActive = campaignActive,
            // Registra el id base de la carta jugada (sin el sufijo de posición) para los logros.
            cartasJugadas = state.cartasJugadas + card.id.substringBeforeLast("_"),
            indicators = state.indicators.copy(
                budget = state.indicators.budget - card.cost + extraBudget,
                environmental = amb,
                coverage = coverage(districts)
            )
        )
    }

    /** Bonus/penalización ambiental al jugar una carta de ENERGIA, según su fuente. */
    private fun ambDelta(source: EnergySource?): Int = when (source) {
        EnergySource.SOLAR -> 6
        EnergySource.EOLICA -> 8
        EnergySource.TERMICA -> -16
        EnergySource.BATERIA -> 2
        EnergySource.HIDRO -> 4
        EnergySource.REPARACION, null -> 0
    }

    // ────────────────────────── Encender / apagar distrito ─────────────────────

    /** Cambia el estado encendido/apagado de un distrito y recalcula la cobertura. */
    fun setDistrictPower(state: GameState, districtId: String, on: Boolean): GameState {
        val districts = state.districts.map { d ->
            if (d.id == districtId && !d.lost) d.copy(on = on) else d
        }
        return state.copy(
            districts = districts,
            indicators = state.indicators.copy(coverage = coverage(districts))
        )
    }

    // ───────────────────────────── Progresión (puro) ───────────────────────────

    /** Promedio de los 4 indicadores (presupuesto se acota a 0..100 para el promedio). */
    fun promedioIndicadores(state: GameState): Int {
        val i = state.indicators
        return (i.coverage + i.budget.coerceIn(0, 100) + i.social + i.environmental) / 4
    }

    /** Medalla final según el promedio de indicadores. */
    fun medalla(state: GameState): Medalla = when {
        promedioIndicadores(state) >= 60 -> Medalla.ORO
        promedioIndicadores(state) >= 40 -> Medalla.PLATA
        else -> Medalla.BRONCE
    }

    /**
     * Vatios ganados al terminar la partida, con desglose para animarlo en la pantalla final.
     *   vatios = promedio*2 + noches*10 + bonusMedalla + (supervivencia: noches*15)
     */
    fun calcVatios(state: GameState): VatiosBreakdown {
        val noches = state.day
        val medallaBonus = when (medalla(state)) {
            Medalla.ORO -> 100
            Medalla.PLATA -> 50
            Medalla.BRONCE -> 20
        }
        val supervivencia = if (state.mode == GameMode.SUPERVIVENCIA) noches * 15 else 0
        return VatiosBreakdown(
            indicadores = promedioIndicadores(state) * 2,
            noches = noches * 10,
            medalla = medallaBonus,
            supervivencia = supervivencia
        )
    }

    /**
     * Aplica las mejoras compradas a un GameState recién creado (antes de [startNight]).
     * Presupuesto y bienestar son de una sola vez; cuadrillas y energía base se guardan como
     * bonus por-noche que [startNight]/[baseEnergy] vuelven a aplicar cada noche.
     */
    fun aplicarMejoras(state: GameState, mejoras: Set<String>): GameState {
        var budget = state.indicators.budget
        var social = state.indicators.social
        var bonusAcc = state.bonusAccionesPorNoche
        var bonusEner = state.bonusEnergiaBase

        if ("presupuesto_inicial" in mejoras) budget = (budget * 1.1).roundToInt()
        if ("colchon_social" in mejoras) social += 10
        if ("cuadrilla_extra" in mejoras) bonusAcc += 1
        if ("energia_base" in mejoras) bonusEner += 5

        return state.copy(
            indicators = state.indicators.copy(budget = budget, social = social),
            bonusAccionesPorNoche = bonusAcc,
            bonusEnergiaBase = bonusEner
        )
    }

    /**
     * Evalúa qué logros SATISFACE esta partida terminada (sin saber cuáles ya están
     * desbloqueados; de eso se encarga el repositorio). Función pura.
     */
    fun evaluarLogros(state: GameState, result: NightResult): Set<Logro> {
        val logros = mutableSetOf<Logro>()

        // Terminar al menos una noche.
        if (state.day >= 1) logros += Logro.PRIMER_APAGON

        // Ganar sin haber apagado nunca el hospital.
        val hospital = state.districts.firstOrNull { it.trait == DistrictTrait.HOSPITAL }
        if (result.victory && hospital != null && hospital.totalOff == 0) {
            logros += Logro.GUARDIAN_HOSPITAL
        }

        // Ganar usando solo energías renovables (nunca térmica/baterías/reparación).
        if (result.victory) {
            val noRenovables = setOf("termica", "baterias", "reparacion")
            val renovables = setOf("solar", "eolica", "hidroelectrica")
            val jugadas = state.cartasJugadas
            if (jugadas.none { it in noRenovables } && jugadas.any { it in renovables }) {
                logros += Logro.CIEN_RENOVABLE
            }
        }

        // Aguantar 7 noches en Supervivencia.
        if (state.mode == GameMode.SUPERVIVENCIA && state.day >= 7) logros += Logro.SUPERVIVIENTE

        // Perder un distrito rural.
        if (state.districts.any { it.trait == DistrictTrait.RURAL && it.lost }) logros += Logro.SACRIFICIO

        // Final Oro.
        if (medalla(state) == Medalla.ORO) logros += Logro.ORO_PURO

        return logros
    }

    // ───────────────────────────── Resolver la noche ───────────────────────────

    /**
     * Calcula las consecuencias de la noche y devuelve un [NightResult].
     * Orden de cálculo según las reglas del juego. NO incrementa el día:
     * eso lo hace el ViewModel en nextNight() al preparar la siguiente noche.
     */
    fun processNightEnd(state: GameState): NightResult {
        var socialPenalty = 0
        var budgetDelta = 0
        var hospitalGameOver = false

        // 1 y 2. Recorremos los distritos actualizando contadores y acumulando penalizaciones.
        //    Para los apagados, consecutiveOff/totalOff se incrementan ANTES de evaluar
        //    (así "2 noches seguidas" del hospital dispara en la 2ª noche apagado).
        val updated = state.districts.map { d ->
            when {
                d.lost -> d
                !d.on -> {
                    val newConsecutive = d.consecutiveOff + 1
                    val newTotal = d.totalOff + 1
                    var lost = false
                    when (d.trait) {
                        DistrictTrait.HOSPITAL -> {
                            socialPenalty += 18
                            if (newConsecutive >= 2) hospitalGameOver = true
                        }
                        DistrictTrait.INDUSTRIAL -> {
                            socialPenalty += 6
                            budgetDelta -= 6
                        }
                        DistrictTrait.RESIDENCIAL -> {
                            socialPenalty += 6 + newConsecutive * 3
                        }
                        DistrictTrait.RURAL -> {
                            socialPenalty += 3
                            if (newTotal >= 3) lost = true
                        }
                    }
                    d.copy(consecutiveOff = newConsecutive, totalOff = newTotal, lost = lost)
                }
                else -> {
                    // Encendido: el industrial aporta presupuesto; se resetea su racha de apagón.
                    if (d.trait == DistrictTrait.INDUSTRIAL) budgetDelta += 6
                    d.copy(consecutiveOff = 0)
                }
            }
        }

        // 3. Festival con el distrito industrial apagado.
        val industrialOff = state.districts.any {
            it.trait == DistrictTrait.INDUSTRIAL && !it.on && !it.lost
        }
        if (state.scenario.isSpecialFestival && industrialOff) {
            socialPenalty += 14
        }

        // 4. Sinergia Campaña Ciudadana: -60% de penalización social.
        if (state.campaignActive) {
            socialPenalty = (socialPenalty * 0.4).roundToInt()
        }

        // 5 y 6. Bienestar social: bonus por "todos encendidos", luego clamp 0..100.
        val allOn = updated.any { !it.lost } && updated.none { !it.lost && !it.on }
        var soc = state.indicators.social - socialPenalty
        if (allOn) soc += 8
        val newSoc = soc.coerceIn(0, 100)

        val newBudget = state.indicators.budget + budgetDelta
        val newCoverage = coverage(updated)
        val amb = state.indicators.environmental

        // Condiciones de derrota.
        val reason: GameOverReason? = when {
            hospitalGameOver -> GameOverReason.HOSPITAL
            newSoc <= 0 -> GameOverReason.BIENESTAR
            newCoverage == 0 -> GameOverReason.COBERTURA
            amb <= 0 -> GameOverReason.AMBIENTAL
            else -> null
        }
        val defeat = reason != null
        // Victoria: solo en campaña, al superar la 5ª noche sin derrota.
        val victory = !defeat && state.mode == GameMode.CAMPANA && state.day >= 5

        val indicators = state.indicators.copy(
            coverage = newCoverage,
            budget = newBudget,
            social = newSoc,
            environmental = amb
        )
        // El día NO se incrementa aquí; nextNight() lo avanza al preparar la siguiente noche.
        val resolvedState = state.copy(
            districts = updated,
            indicators = indicators,
            gameOver = defeat,
            victory = victory,
            gameOverReason = reason
        )

        return NightResult(
            state = resolvedState,
            socialPenalty = socialPenalty,
            budgetDelta = budgetDelta,
            offDistrictIds = state.districts.filter { !it.on && !it.lost }.map { it.id },
            isGameOver = defeat,
            victory = victory,
            gameOverReason = reason
        )
    }
}
