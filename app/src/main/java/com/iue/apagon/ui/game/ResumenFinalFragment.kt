package com.iue.apagon.ui.game

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import com.iue.apagon.databinding.FragmentResumenFinalBinding
import com.iue.apagon.domain.model.GameState

/**
 * Resumen final / victoria (fase "victoria"). Calcula medalla y puntaje a partir de los
 * indicadores finales. Equivale a ResumenFinalActivity del CONTEXT (aquí como Fragment).
 */
class ResumenFinalFragment : Fragment() {

    private var _binding: FragmentResumenFinalBinding? = null
    private val binding get() = _binding!!

    private val host get() = activity as? GameActivity

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResumenFinalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val state = host?.finalState() ?: host?.currentPlaying()?.state ?: return
        render(state)
        binding.btnAgain.setOnClickListener { host?.restart() }
        binding.btnMenu.setOnClickListener { host?.exitToMenu() }
    }

    private fun render(state: GameState) {
        val ind = state.indicators
        val avg = (ind.coverage + ind.budget.coerceIn(0, 100) + ind.social + ind.environmental) / 4

        data class Medal(val emoji: String, val title: String, val color: Int, val sub: String)
        val medal = when {
            avg >= 60 -> Medal("🥇", "Final Oro", Color.parseColor("#F59E0B"), "Transición Energética")
            avg >= 40 -> Medal("🥈", "Final Plata", Color.parseColor("#94A3B8"), "Gestor Competente")
            else -> Medal("🥉", "Final Bronce", Color.parseColor("#B87333"), "Sobreviviste")
        }

        binding.medalEmoji.text = medal.emoji
        binding.medalTitle.text = medal.title
        binding.medalTitle.setTextColor(medal.color)
        binding.medalSubtitle.text = "${medal.sub} · ${state.municipio.displayName}"

        binding.scoreValue.text = (avg * 130).toString()
        binding.scoreValue.setTextColor(medal.color)

        // Acentos del fondo / tarjeta con el color de la medalla.
        binding.resumenRoot.setBackgroundColor(
            ColorUtils.blendARGB(Color.parseColor("#060912"), medal.color, 0.08f)
        )
        (binding.scoreValue.parent as? View)?.backgroundTintList =
            ColorStateList.valueOf(ColorUtils.setAlphaComponent(medal.color, 0x14))
        binding.btnAgain.backgroundTintList = ColorStateList.valueOf(medal.color)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
