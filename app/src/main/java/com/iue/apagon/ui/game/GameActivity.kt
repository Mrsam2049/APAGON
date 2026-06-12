package com.iue.apagon.ui.game

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.iue.apagon.R
import com.iue.apagon.data.repository.GameRepository
import com.iue.apagon.databinding.ActivityGameBinding
import com.iue.apagon.domain.engine.Logro
import com.iue.apagon.domain.engine.NightResult
import com.iue.apagon.domain.engine.VatiosBreakdown
import com.iue.apagon.domain.model.GameMode
import com.iue.apagon.domain.model.GameState
import com.iue.apagon.domain.model.Municipio
import com.iue.apagon.ui.viewmodel.GameUiState
import com.iue.apagon.ui.viewmodel.GameViewModel
import com.iue.apagon.ui.viewmodel.UiEvent
import kotlinx.coroutines.launch

/**
 * Host de la partida. Posee el [GameViewModel], observa su estado y va cambiando el Fragment
 * mostrado (escenario → gestión → reporte → derrota/victoria). Reemplaza la máquina de fases
 * "phase" del prototipo React.
 */
class GameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameBinding

    val viewModel: GameViewModel by viewModels {
        GameViewModel.factory(GameRepository.get(applicationContext))
    }

    private lateinit var mode: GameMode
    private lateinit var municipio: Municipio

    private var shownDay = 0
    private var lastPlaying: GameUiState.Playing? = null
    private var lastReport: NightResult? = null
    private var lastFinalState: GameState? = null
    private var lastVatios: VatiosBreakdown? = null
    private var lastLogros: List<Logro> = emptyList()

    fun currentPlaying(): GameUiState.Playing? = lastPlaying
    fun currentReport(): NightResult? = lastReport
    fun finalState(): GameState? = lastFinalState
    fun vatios(): VatiosBreakdown? = lastVatios
    fun logros(): List<Logro> = lastLogros

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mode = GameMode.valueOf(intent.getStringExtra(EXTRA_MODE) ?: GameMode.CAMPANA.name)
        municipio = Municipio.valueOf(intent.getStringExtra(EXTRA_MUNICIPIO) ?: Municipio.APARTADO.name)
        shownDay = savedInstanceState?.getInt(STATE_SHOWN_DAY, 0) ?: 0

        binding.loadingCity.text = municipio.displayName
        binding.loadingMode.text = if (mode == GameMode.CAMPANA) "📖 Campaña" else "🔥 Supervivencia"

        observeState()
        observeEvents()

        if (savedInstanceState == null) viewModel.startGame(mode, municipio)
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> renderState(state) }
            }
        }
    }

    private fun observeEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    val msg = when (event) {
                        UiEvent.OverloadError -> "⚠ Sobrecarga: apaga un distrito o juega energía"
                        UiEvent.CardBlocked -> "No puedes jugar esa carta (sin acciones o presupuesto)"
                        is UiEvent.LogroDesbloqueado ->
                            "${event.logro.icono} Logro: ${event.logro.titulo}  +${event.logro.vatios} ⚡"
                    }
                    val dur = if (event is UiEvent.LogroDesbloqueado) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                    Toast.makeText(this@GameActivity, msg, dur).show()
                }
            }
        }
    }

    private fun renderState(state: GameUiState) {
        binding.loadingOverlay.visibility =
            if (state is GameUiState.Loading) android.view.View.VISIBLE else android.view.View.GONE

        when (state) {
            GameUiState.Loading -> Unit

            is GameUiState.Playing -> {
                lastPlaying = state
                if (state.state.day != shownDay) {
                    shownDay = state.state.day
                    show(EscenarioNocheFragment())
                } else {
                    when (val current = currentFragment()) {
                        is GestionRedFragment -> current.render(state)
                        is EscenarioNocheFragment -> Unit // se queda en la intro hasta pulsar "Gestionar"
                        else -> showGestionRed()
                    }
                }
            }

            is GameUiState.NightReport -> {
                lastReport = state.result
                lastFinalState = state.state
                show(ReporteNocturnoFragment())
            }

            is GameUiState.GameOver -> {
                lastReport = state.result
                lastFinalState = state.state
                lastVatios = state.vatios
                lastLogros = state.logros
                show(GameOverFragment())
            }

            is GameUiState.Victory -> {
                lastFinalState = state.state
                lastVatios = state.vatios
                lastLogros = state.logros
                show(ResumenFinalFragment())
            }
        }
    }

    private fun currentFragment() = supportFragmentManager.findFragmentById(R.id.gameContainer)

    private fun show(fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager.commit { replace(R.id.gameContainer, fragment) }
    }

    /** Llamado por EscenarioNocheFragment al pulsar "Gestionar la red". */
    fun showGestionRed() {
        show(GestionRedFragment())
    }

    /** Reinicia la partida con el mismo modo/municipio. */
    fun restart() {
        shownDay = 0
        lastReport = null
        lastFinalState = null
        viewModel.startGame(mode, municipio)
    }

    fun exitToMenu() {
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_SHOWN_DAY, shownDay)
    }

    companion object {
        const val EXTRA_MODE = "extra_mode"
        const val EXTRA_MUNICIPIO = "extra_municipio"
        private const val STATE_SHOWN_DAY = "shown_day"
    }
}
