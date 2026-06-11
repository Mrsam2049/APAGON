package com.iue.apagon.ui.viewmodel

import com.iue.apagon.domain.engine.NightResult
import com.iue.apagon.domain.model.GameState

/**
 * Estados de la pantalla de juego. La UI observa uiState y renderiza según el subtipo.
 */
sealed interface GameUiState {

    /** Cargando datos iniciales (fetch de energía + armado del estado). */
    data object Loading : GameUiState

    /**
     * Partida en curso. [effectiveAvailable] es la energía disponible total
     * (base del escenario + bonus de cartas) para contrastar con la demanda encendida.
     */
    data class Playing(
        val state: GameState,
        val effectiveAvailable: Int
    ) : GameUiState

    /** Reporte tras resolver una noche (cuando la partida continúa). */
    data class NightReport(
        val state: GameState,
        val result: NightResult
    ) : GameUiState

    /** Derrota: algún indicador llegó a 0 o el hospital cayó 2 noches seguidas. */
    data class GameOver(
        val state: GameState,
        val result: NightResult
    ) : GameUiState

    /** Victoria de campaña (5 noches superadas sin derrota). */
    data class Victory(
        val state: GameState
    ) : GameUiState
}
