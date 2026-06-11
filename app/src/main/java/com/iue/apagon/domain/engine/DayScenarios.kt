package com.iue.apagon.domain.engine

import kotlin.random.Random

/**
 * Catálogo de escenarios nocturnos. Las 5 noches de campaña usan, en orden, los escenarios
 * de [campaign]. Los modificadores de demanda apuntan a ids de distritos de Apartadó
 * (centro, norte, bananera); en otros municipios simplemente no aplican.
 */
object DayScenarios {

    val NOCHE_TRANQUILA = NightScenario(
        id = "noche_tranquila",
        title = "Noche Tranquila",
        icon = "🌙",
        energyBaseMw = 72,
        description = "Una noche sin sobresaltos. La demanda es estable y hay margen para respirar.",
        demandModifiers = emptyMap()
    )

    val OLA_DE_CALOR = NightScenario(
        id = "ola_de_calor",
        title = "Ola de Calor",
        icon = "🔥",
        energyBaseMw = 70,
        description = "El calor dispara ventiladores y aires acondicionados. El centro y el norte consumen mucho más.",
        demandModifiers = mapOf("centro" to 1.5f, "norte" to 1.4f)
    )

    val FALLA_SUBESTACION = NightScenario(
        id = "falla_subestacion",
        title = "Falla en Subestación",
        icon = "⚡",
        energyBaseMw = 54,
        description = "Una subestación cayó. La energía disponible se desploma esta noche.",
        demandModifiers = emptyMap()
    )

    val FESTIVAL = NightScenario(
        id = "festival",
        title = "Festival",
        icon = "🎉",
        energyBaseMw = 74,
        description = "Las fiestas llenan las calles. Si apagas la zona industrial, el descontento se dispara.",
        demandModifiers = mapOf("bananera" to 1.3f),
        isSpecialFestival = true
    )

    val SEQUIA_CRITICA = NightScenario(
        id = "sequia_critica",
        title = "Sequía Crítica",
        icon = "🏜️",
        energyBaseMw = 46,
        description = "Los embalses están en mínimos por El Niño. Apenas hay energía para lo esencial.",
        demandModifiers = emptyMap()
    )

    /** Escenarios de las 5 noches de campaña, en orden. */
    val campaign: List<NightScenario> = listOf(
        NOCHE_TRANQUILA,
        OLA_DE_CALOR,
        FALLA_SUBESTACION,
        FESTIVAL,
        SEQUIA_CRITICA
    )

    /** Escenario para una noche concreta de campaña (day en 1..5). */
    fun forDay(day: Int): NightScenario = campaign.getOrElse(day - 1) { campaign.last() }

    /** Escenario aleatorio, útil para el modo Supervivencia. */
    fun random(rng: Random = Random.Default): NightScenario = campaign.random(rng)
}
