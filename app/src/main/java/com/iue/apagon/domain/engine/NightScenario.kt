package com.iue.apagon.domain.engine

/**
 * Escenario de una noche (se muestra en EscenarioNocheFragment).
 *
 * - [energyBaseMw]: energía base antes de la degradación de El Niño.
 * - [demandModifiers]: multiplicadores de demanda por id de distrito (1.5f = +50%).
 *   Los distritos no listados conservan su demanda base. El engine los aplica en startNight.
 * - [isSpecialFestival]: si es true y el distrito industrial queda apagado, +14 de penalización social.
 */
data class NightScenario(
    val id: String,
    val title: String,
    val icon: String,
    val energyBaseMw: Int,
    val description: String,
    val demandModifiers: Map<String, Float> = emptyMap(),
    val isSpecialFestival: Boolean = false
)
