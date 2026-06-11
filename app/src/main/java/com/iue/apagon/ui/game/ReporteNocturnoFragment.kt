package com.iue.apagon.ui.game

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.iue.apagon.R
import com.iue.apagon.databinding.FragmentReporteNocturnoBinding
import com.iue.apagon.domain.engine.NightResult
import com.iue.apagon.domain.model.DistrictTrait
import com.iue.apagon.domain.model.GameMode
import com.iue.apagon.ui.Presentation

/**
 * Reporte de la noche (fase "resultado"). Reconstruye los hechos a partir del NightResult
 * y del estado resuelto, y permite avanzar a la siguiente noche.
 */
class ReporteNocturnoFragment : Fragment() {

    private var _binding: FragmentReporteNocturnoBinding? = null
    private val binding get() = _binding!!

    private val host get() = activity as? GameActivity

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReporteNocturnoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val result = host?.currentReport() ?: return
        render(result)
        binding.btnNext.setOnClickListener { host?.viewModel?.nextNight() }
    }

    private fun render(result: NightResult) {
        val ctx = requireContext()
        val state = result.state
        val ind = state.indicators

        binding.reportHeader.text = "AMANECE · FIN DE LA NOCHE ${state.day}"

        binding.sumCoverage.text = ctx.getString(R.string.percent_fmt, ind.coverage)
        binding.sumCoverage.setTextColor(
            ContextCompat.getColor(
                ctx,
                when {
                    ind.coverage > 75 -> R.color.green
                    ind.coverage > 50 -> R.color.amber
                    else -> R.color.red
                }
            )
        )
        binding.sumSocial.text = "−${result.socialPenalty}"
        binding.sumSocial.setTextColor(
            ContextCompat.getColor(ctx, if (result.socialPenalty == 0) R.color.green else R.color.red)
        )
        val deltaSign = if (result.budgetDelta >= 0) "+" else ""
        binding.sumBudget.text = "$deltaSign${result.budgetDelta}"
        binding.sumBudget.setTextColor(
            ContextCompat.getColor(ctx, if (result.budgetDelta >= 0) R.color.green else R.color.red)
        )

        binding.campaignNote.visibility =
            if (state.campaignActive && result.socialPenalty > 0) View.VISIBLE else View.GONE

        // Eventos reconstruidos a partir del estado resuelto.
        val eventos = buildList {
            val festivalIndustrialOff = state.scenario.isSpecialFestival &&
                state.districts.any { it.trait == DistrictTrait.INDUSTRIAL && !it.on && !it.lost }

            state.districts.forEach { d ->
                when {
                    d.lost -> add(ReporteEvento(Presentation.traitIcon(d.trait), "${d.name} perdida: fuera de la red.", true))
                    !d.on -> add(ReporteEvento(Presentation.traitIcon(d.trait), "${d.name} a oscuras.", true))
                    d.trait == DistrictTrait.INDUSTRIAL ->
                        add(ReporteEvento(Presentation.traitIcon(d.trait), "${d.name} produciendo: +\$6M.", false))
                }
            }
            if (festivalIndustrialOff) {
                add(ReporteEvento("🎉", "¡Apagaste el festival! Indignación masiva.", true))
            }
            val allOn = state.districts.any { !it.lost } && state.districts.none { !it.lost && !it.on }
            if (allOn) add(ReporteEvento("✓", "Ciudad completa con energía. +8 social.", false))
        }

        binding.eventList.layoutManager = LinearLayoutManager(ctx)
        binding.eventList.adapter = EventoAdapter(eventos)

        val isLastCampaign = state.mode == GameMode.CAMPANA && state.day >= 5
        binding.btnNext.text = if (isLastCampaign) "Ver final →" else "Siguiente noche →"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
