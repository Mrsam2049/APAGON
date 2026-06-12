package com.iue.apagon.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.iue.apagon.data.repository.GameRepository
import com.iue.apagon.domain.engine.CardDeck
import com.iue.apagon.domain.engine.DayScenarios
import com.iue.apagon.domain.engine.GameEngine
import com.iue.apagon.domain.engine.MunicipioData
import com.iue.apagon.domain.model.Card
import com.iue.apagon.domain.model.GameMode
import com.iue.apagon.domain.model.GameState
import com.iue.apagon.domain.model.Municipio
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel principal de la partida. Conecta el GameEngine (reglas puras) con el
 * GameRepository (red + Room) y expone:
 *  - [uiState]: estado de pantalla observable.
 *  - [events]: eventos de una sola vez (errores transitorios).
 */
class GameViewModel(
    private val repository: GameRepository
) : ViewModel() {

    private val engine = GameEngine()

    private val _uiState = MutableStateFlow<GameUiState>(GameUiState.Loading)
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    /** Estado de juego vivo: fuente de verdad del modelo. */
    private var game: GameState? = null

    /** Ids de cartas desbloqueadas en el perfil (se aplican al repartir manos). */
    private var unlockedCardIds: Set<String> = emptySet()

    /** Evita resolver dos veces la noche final mientras se persiste de forma asíncrona. */
    private var finishing = false

    // 1) Inicia una nueva partida ------------------------------------------------
    fun startGame(mode: GameMode, municipio: Municipio) {
        _uiState.value = GameUiState.Loading
        finishing = false
        viewModelScope.launch {
            // Refresca y cachea datos de energía; el juego sigue siendo jugable aunque falle.
            repository.fetchEnergyData()

            // Perfil persistente: cartas desbloqueadas + mejoras compradas.
            val perfil = repository.perfil()
            unlockedCardIds = csv(perfil.cartasDesbloqueadas)
            val mejoras = csv(perfil.mejorasCompradas)

            val scenario = when (mode) {
                GameMode.CAMPANA -> DayScenarios.forDay(1)
                GameMode.SUPERVIVENCIA -> DayScenarios.random()
            }
            val initial = GameState(
                mode = mode,
                municipio = municipio,
                day = 1,
                scenario = scenario,
                districts = MunicipioData.districtsFor(municipio),
                hand = CardDeck.drawWeighted(HAND_SIZE, unlockedCardIds)
            )
            // Aplica las mejoras compradas y luego prepara la primera noche.
            val conMejoras = engine.aplicarMejoras(initial, mejoras)
            val ready = engine.startNight(conMejoras)
            game = ready
            emitPlaying(ready)
        }
    }

    // 2) Enciende/apaga un distrito ---------------------------------------------
    fun toggleDistrict(districtId: String) {
        if (_uiState.value !is GameUiState.Playing) return
        val state = game ?: return
        val district = state.districts.firstOrNull { it.id == districtId } ?: return
        if (district.lost) return

        // Solo se valida la capacidad al ENCENDER (apagar siempre reduce la carga).
        if (!district.on) {
            val effectiveAvailable = engine.availableEnergy(state)
            if (state.poweredDemand + district.demand > effectiveAvailable) {
                sendEvent(UiEvent.OverloadError)
                return
            }
        }
        val newState = engine.setDistrictPower(state, districtId, !district.on)
        game = newState
        emitPlaying(newState)
    }

    // 3) Juega una carta de la mano ---------------------------------------------
    fun playCard(card: Card) {
        if (_uiState.value !is GameUiState.Playing) return
        val state = game ?: return
        if (state.actionPoints <= 0 || card.cost > state.indicators.budget) {
            sendEvent(UiEvent.CardBlocked)
            return
        }
        val newState = engine.applyCardEffect(state, card)
        game = newState
        emitPlaying(newState)
    }

    // 4) Cierra la noche y resuelve consecuencias -------------------------------
    fun endNight() {
        if (_uiState.value !is GameUiState.Playing || finishing) return
        val state = game ?: return

        // No se puede cerrar la noche con la red sobrecargada.
        if (state.poweredDemand > engine.availableEnergy(state)) {
            sendEvent(UiEvent.OverloadError)
            return
        }

        val result = engine.processNightEnd(state)
        val resolved = result.state
        game = resolved

        if (result.isGameOver || result.victory) {
            finishing = true
            // Persiste, acredita Vatios y desbloquea logros ANTES de emitir el estado final
            // (así la pantalla de fin ya trae los Vatios y los logros nuevos para mostrarlos).
            val vatios = engine.calcVatios(resolved)
            viewModelScope.launch {
                repository.savePartida(resolved, resolved.mode, resolved.municipio)
                repository.addVatios(vatios.total)
                val nuevosLogros = repository.desbloquearLogros(engine.evaluarLogros(resolved, result))
                nuevosLogros.forEach { repository.addVatios(it.vatios) }

                _uiState.value = if (result.victory) {
                    GameUiState.Victory(resolved, vatios, nuevosLogros)
                } else {
                    GameUiState.GameOver(resolved, result, vatios, nuevosLogros)
                }
                nuevosLogros.forEach { _events.emit(UiEvent.LogroDesbloqueado(it)) }
            }
        } else {
            _uiState.value = GameUiState.NightReport(resolved, result)
        }
    }

    // 5) Avanza a la siguiente noche --------------------------------------------
    fun nextNight() {
        if (_uiState.value !is GameUiState.NightReport) return
        val state = game ?: return

        val nextDay = state.day + 1
        val scenario = when (state.mode) {
            GameMode.CAMPANA -> DayScenarios.forDay(nextDay)
            GameMode.SUPERVIVENCIA -> DayScenarios.random()
        }
        val prepared = state.copy(
            day = nextDay,
            scenario = scenario,
            hand = CardDeck.drawWeighted(HAND_SIZE, unlockedCardIds)
        )
        // startNight aplica el ingreso diario al presupuesto y reinicia la noche.
        val ready = engine.startNight(prepared)
        game = ready
        emitPlaying(ready)
    }

    // Helpers --------------------------------------------------------------------
    private fun emitPlaying(state: GameState) {
        _uiState.value = GameUiState.Playing(
            state = state,
            effectiveAvailable = engine.availableEnergy(state)
        )
    }

    private fun sendEvent(event: UiEvent) {
        viewModelScope.launch { _events.emit(event) }
    }

    private fun csv(value: String): Set<String> =
        value.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()

    companion object {
        private const val HAND_SIZE = 4

        /** Factory para instanciar el ViewModel sin Hilt, inyectando el repositorio. */
        fun factory(repository: GameRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { GameViewModel(repository) }
        }
    }
}
