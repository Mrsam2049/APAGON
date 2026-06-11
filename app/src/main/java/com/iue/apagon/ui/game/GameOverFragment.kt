package com.iue.apagon.ui.game

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.iue.apagon.databinding.FragmentGameOverBinding
import com.iue.apagon.domain.model.GameMode
import com.iue.apagon.domain.model.GameOverReason

/**
 * Pantalla de derrota (fase "derrota"). Muestra el motivo y, en supervivencia, las noches
 * sobrevividas. Equivale a GameOverFragment del CONTEXT.
 */
class GameOverFragment : Fragment() {

    private var _binding: FragmentGameOverBinding? = null
    private val binding get() = _binding!!

    private val host get() = activity as? GameActivity

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGameOverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val state = host?.finalState()
        val reason = state?.gameOverReason

        binding.gameOverReason.text = reasonText(reason)

        if (state?.mode == GameMode.SUPERVIVENCIA) {
            binding.survivalBox.visibility = View.VISIBLE
            binding.survivalNights.text = state.day.toString()
        } else {
            binding.survivalBox.visibility = View.GONE
        }

        binding.btnRetry.setOnClickListener { host?.restart() }
        binding.btnMenu.setOnClickListener { host?.exitToMenu() }
    }

    private fun reasonText(reason: GameOverReason?): String = when (reason) {
        GameOverReason.HOSPITAL ->
            "El hospital llevó 2 noches sin energía. Hubo muertes evitables y la ciudad estalló."
        GameOverReason.BIENESTAR ->
            "El descontento social desbordó: la ciudadanía se levantó contra el racionamiento."
        GameOverReason.COBERTURA ->
            "La cobertura cayó a cero. La ciudad quedó completamente a oscuras."
        GameOverReason.AMBIENTAL ->
            "El índice ambiental colapsó por la quema térmica. Crisis ecológica."
        null -> "Un indicador llegó a cero."
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
