package com.iue.apagon.domain.engine

/** Medalla final según el promedio de indicadores. */
enum class Medalla { ORO, PLATA, BRONCE }

/**
 * Desglose de Vatios ganados al terminar una partida (para mostrarlo animado).
 * [total] es la suma que se acredita al perfil.
 */
data class VatiosBreakdown(
    val indicadores: Int,    // promedio * 2
    val noches: Int,         // noches alcanzadas * 10
    val medalla: Int,        // bonus por medalla (oro 100 / plata 50 / bronce 20)
    val supervivencia: Int   // solo supervivencia: noches * 15 extra
) {
    val total: Int get() = indicadores + noches + medalla + supervivencia
}
