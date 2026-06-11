package com.iue.apagon.ui.game

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import com.iue.apagon.databinding.FragmentEscenarioNocheBinding
import com.iue.apagon.domain.model.GameMode
import com.iue.apagon.ui.Presentation
import com.iue.apagon.ui.viewmodel.GameUiState

/**
 * Intro de la noche ("dayIntro"): anuncia el escenario. Lee el GameState actual del host
 * y, al pulsar "Gestionar la red", le pide a [GameActivity] mostrar la gestión.
 */
class EscenarioNocheFragment : Fragment() {

    private var _binding: FragmentEscenarioNocheBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEscenarioNocheBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        render()
        binding.btnGestionar.setOnClickListener {
            (activity as? GameActivity)?.showGestionRed()
        }
    }

    private fun render() {
        val playing = (activity as? GameActivity)?.currentPlaying() ?: return
        val state = playing.state
        val sc = state.scenario
        val accent = Color.parseColor(Presentation.scenarioColor(sc))

        val nocheTxt = if (state.mode == GameMode.CAMPANA) "NOCHE ${state.day}/5" else "NOCHE ${state.day}"
        binding.header.text = "${state.municipio.displayName.uppercase()} · $nocheTxt"
        binding.scenarioIcon.text = sc.icon
        binding.scenarioTitle.text = sc.title
        binding.scenarioTitle.setTextColor(accent)
        binding.scenarioText.text = sc.description
        binding.statEnergy.text = sc.energyBaseMw.toString()
        binding.statBudget.text = "$${state.indicators.budget}M"

        // Fondo con un leve tinte del color del escenario.
        binding.root.setBackgroundColor(
            ColorUtils.blendARGB(Color.parseColor("#060912"), accent, 0.08f)
        )
        binding.btnGestionar.backgroundTintList =
            android.content.res.ColorStateList.valueOf(accent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun forState(@Suppress("UNUSED_PARAMETER") state: GameUiState.Playing) =
            EscenarioNocheFragment()
    }
}
