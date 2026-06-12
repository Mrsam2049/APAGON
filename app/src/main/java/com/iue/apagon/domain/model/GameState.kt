package com.iue.apagon.domain.model

import com.iue.apagon.domain.engine.NightScenario

/**
 * Indicadores principales mostrados al jugador.
 * coverage/social/environmental se mueven en el rango 0..100.
 * budget (millones de pesos) puede crecer libremente y NO es condición de derrota.
 */
data class Indicators(
    val coverage: Int = 100,        // calculado desde los distritos encendidos
    val budget: Int = 100,          // presupuesto en millones
    val social: Int = 100,          // bienestar social (soc)
    val environmental: Int = 100    // índice ambiental (amb)
)

/**
 * Estado completo de la partida en curso. Inmutable: el GameEngine devuelve copias,
 * nunca muta el objeto recibido.
 *
 * - [bonusEnergy]: energía extra aportada por cartas durante la noche actual.
 * - [campaignActive]: sinergia Campaña Ciudadana pendiente de aplicar al resolver la noche.
 * - [actionPoints]: cartas que aún pueden jugarse esta noche (2 por noche).
 */
data class GameState(
    val mode: GameMode,
    val municipio: Municipio,
    val day: Int = 1,
    val scenario: NightScenario,
    val districts: List<District> = emptyList(),
    val hand: List<Card> = emptyList(),
    val deck: List<Card> = emptyList(),
    val actionPoints: Int = 2,
    val bonusEnergy: Int = 0,
    val bonusAccionesPorNoche: Int = 0,   // mejora "cuadrilla_extra" (+1 jugada por noche)
    val bonusEnergiaBase: Int = 0,        // mejora "energia_base" (+MW disponibles por noche)
    val campaignActive: Boolean = false,
    val cartasJugadas: List<String> = emptyList(),  // ids base de cartas jugadas en la partida (logros)
    val indicators: Indicators = Indicators(),
    val gameOver: Boolean = false,
    val victory: Boolean = false,
    val gameOverReason: GameOverReason? = null
) {
    val totalNights: Int
        get() = if (mode == GameMode.CAMPANA) 5 else Int.MAX_VALUE

    /** Demanda total de los distritos que siguen en juego (no perdidos). */
    val totalDemand: Int
        get() = districts.filter { !it.lost }.sumOf { it.demand }

    /** Demanda cubierta: distritos en juego y encendidos. */
    val poweredDemand: Int
        get() = districts.filter { !it.lost && it.on }.sumOf { it.demand }
}
