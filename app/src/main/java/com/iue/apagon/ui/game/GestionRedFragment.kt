package com.iue.apagon.ui.game

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.iue.apagon.R
import com.iue.apagon.databinding.FragmentGestionRedBinding
import com.iue.apagon.domain.model.GameMode
import com.iue.apagon.ui.viewmodel.GameUiState

/**
 * Pantalla de gestión de red (fase "play"): indicadores, carga/disponible, grid de distritos
 * y mano de cartas. Lee el estado del host ([GameActivity]) y delega las acciones al ViewModel.
 */
class GestionRedFragment : Fragment() {

    private var _binding: FragmentGestionRedBinding? = null
    private val binding get() = _binding!!

    private lateinit var districtAdapter: DistrictAdapter
    private lateinit var cardAdapter: CardAdapter

    private val host get() = activity as? GameActivity

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGestionRedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        districtAdapter = DistrictAdapter { id -> host?.viewModel?.toggleDistrict(id) }
        cardAdapter = CardAdapter { card -> host?.viewModel?.playCard(card) }

        binding.districtGrid.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.districtGrid.adapter = districtAdapter
        binding.districtGrid.isNestedScrollingEnabled = false

        binding.handList.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.handList.adapter = cardAdapter

        binding.btnEndNight.setOnClickListener { host?.viewModel?.endNight() }

        host?.currentPlaying()?.let { render(it) }
    }

    /** Re-renderiza con el último estado Playing (lo invoca el host al cambiar uiState). */
    fun render(playing: GameUiState.Playing) {
        if (_binding == null) return
        val state = playing.state
        val ind = state.indicators
        val effective = playing.effectiveAvailable
        val ctx = requireContext()

        // Cabecera.
        binding.headerIcon.text = state.scenario.icon
        binding.headerTitle.text = state.scenario.title
        binding.headerSub.text = "${state.municipio.displayName} · Noche ${state.day}"
        binding.tagBudget.text = "💰 $${ind.budget}M"
        binding.tagPoints.text = "⚡ ${state.actionPoints}"

        // Indicadores (4 gauges).
        binding.gaugeCob.setup("💡", "COB")
        binding.gaugeCob.setValue(ind.coverage)
        binding.gaugePre.setup("💰", "PRE")
        binding.gaugePre.setValue(ind.budget.coerceIn(0, 100))
        binding.gaugeSoc.setup("😤", "SOC")
        binding.gaugeSoc.setValue(ind.social)
        binding.gaugeAmb.setup("🌿", "AMB")
        binding.gaugeAmb.setValue(ind.environmental)

        // Carga / disponible.
        val powered = state.poweredDemand
        val overload = powered > effective
        val energyColorRes = when {
            overload -> R.color.red
            effective - powered < 10 -> R.color.amber
            else -> R.color.green
        }
        val energyColor = ContextCompat.getColor(ctx, energyColorRes)
        binding.energyValue.text = "$powered / $effective MW"
        binding.energyValue.setTextColor(energyColor)
        binding.energyBar.setIndicatorColor(energyColor)
        val pct = if (effective > 0) (powered * 100 / effective).coerceIn(0, 100) else 100
        binding.energyBar.setProgressCompat(pct, true)

        val bonus = state.bonusEnergy
        when {
            overload -> {
                binding.energyNote.visibility = View.VISIBLE
                binding.energyNote.text = getString(R.string.sobrecarga_aviso)
                binding.energyNote.setTextColor(ContextCompat.getColor(ctx, R.color.red))
            }
            bonus > 0 -> {
                binding.energyNote.visibility = View.VISIBLE
                binding.energyNote.text = "+$bonus MW de cartas"
                binding.energyNote.setTextColor(ContextCompat.getColor(ctx, R.color.green))
            }
            else -> binding.energyNote.visibility = View.GONE
        }

        // Distritos.
        districtAdapter.submit(state.districts)

        // Mano.
        binding.handLabel.text = "🃏 MANO · ${state.actionPoints} jugadas"
        binding.campaignBadge.visibility = if (state.campaignActive) View.VISIBLE else View.GONE
        cardAdapter.submit(state.hand, state.actionPoints, ind.budget)

        // Botón terminar noche.
        binding.btnEndNight.isEnabled = !overload
        if (overload) {
            binding.btnEndNight.setBackgroundResource(R.drawable.btn_disabled)
            binding.btnEndNight.text = getString(R.string.resolver_sobrecarga)
            binding.btnEndNight.setTextColor(ContextCompat.getColor(ctx, R.color.text_dim))
        } else {
            binding.btnEndNight.setBackgroundResource(R.drawable.btn_primary)
            binding.btnEndNight.text = getString(R.string.terminar_noche)
            binding.btnEndNight.setTextColor(Color.WHITE)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun forMode(@Suppress("UNUSED_PARAMETER") mode: GameMode) = GestionRedFragment()
    }
}
