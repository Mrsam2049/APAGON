package com.iue.apagon.domain.engine

import com.iue.apagon.domain.model.GameOverReason
import com.iue.apagon.domain.model.GameState

/**
 * Resultado de resolver una noche ([GameEngine.processNightEnd]).
 * [state] es el GameState con las consecuencias ya aplicadas (el día NO avanza aquí);
 * el resto es el desglose para el ReporteNocturno.
 */
data class NightResult(
    val state: GameState,
    val socialPenalty: Int,           // penalización social final (ya con la sinergia aplicada)
    val budgetDelta: Int,             // variación de presupuesto por distritos
    val offDistrictIds: List<String>, // distritos que quedaron apagados esta noche
    val isGameOver: Boolean,
    val victory: Boolean,
    val gameOverReason: GameOverReason?
)
