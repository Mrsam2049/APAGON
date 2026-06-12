package com.iue.apagon.domain.model

/**
 * Categoría de la carta. Determina qué efecto aplica el GameEngine al jugarse.
 */
enum class CardType {
    ENERGIA,            // suma energía disponible (bonusEnergy) y afecta el índice ambiental
    RACIONAMIENTO,      // reduce la demanda de todos los distritos esta noche
    CAMPANA_CIUDADANA,  // sinergia: activa la reducción de penalización social
    EFICIENCIA,         // (desbloqueable) reduce 30% la demanda de todos los distritos
    SUBSIDIO            // (desbloqueable) inyecta presupuesto inmediato
}

/**
 * Fuente de una carta de ENERGIA. Define el bonus ambiental al jugarla.
 */
enum class EnergySource {
    SOLAR,        // amb +6
    EOLICA,       // amb +8
    TERMICA,      // amb -16
    REPARACION,   // amb +0
    BATERIA,      // amb +2
    HIDRO         // amb +4 (desbloqueable)
}

/**
 * Carta de la mano del jugador.
 *
 * - [mw]: energía aportada (solo cartas ENERGIA).
 * - [source]: fuente de energía (solo cartas ENERGIA), define el efecto ambiental.
 * - [cost]: costo en millones que se descuenta del presupuesto al jugarla (0 = gratis).
 */
data class Card(
    val id: String,
    val name: String,
    val type: CardType,
    val description: String,
    val mw: Int = 0,
    val source: EnergySource? = null,
    val cost: Int = 0
)
