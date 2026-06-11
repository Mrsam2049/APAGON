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
    fun baseEnergy(state: GameState): Int = when (state.mode) {
        GameMode.CAMPANA -> state.scenario.energyBaseMw
        GameMode.SUPERVIVENCIA ->
            maxOf(38, state.scenario.energyBaseMw - (state.day - 1) * 4)
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
            actionPoints = 2,
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
            CardType.CAMPANA_CIUDADANA -> campaignActive = true
        }

        return state.copy(
            districts = districts,
            hand = state.hand - card,
            actionPoints = (state.actionPoints - 1).coerceAtLeast(0),
            bonusEnergy = bonusEnergy,
            campaignActive = campaignActive,
            indicators = state.indicators.copy(
                budget = state.indicators.budget - card.cost,
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
