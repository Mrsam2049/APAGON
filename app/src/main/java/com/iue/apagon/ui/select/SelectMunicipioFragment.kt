package com.iue.apagon.ui.select

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.iue.apagon.databinding.FragmentSelectMunicipioBinding
import com.iue.apagon.domain.model.GameMode
import com.iue.apagon.domain.model.Municipio
import com.iue.apagon.ui.game.GameActivity

/**
 * Selección de municipio (HU-01): un RecyclerView con las 3 ciudades. Al elegir, lanza
 * GameActivity con el modo + municipio. Equivale a la fase "city" del prototipo.
 */
class SelectMunicipioFragment : Fragment() {

    private var _binding: FragmentSelectMunicipioBinding? = null
    private val binding get() = _binding!!

    private val mode: GameMode by lazy {
        GameMode.valueOf(requireArguments().getString(ARG_MODE, GameMode.CAMPANA.name))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectMunicipioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        binding.cityList.layoutManager = LinearLayoutManager(requireContext())
        binding.cityList.adapter = CityAdapter { municipio -> launchGame(municipio) }
    }

    private fun launchGame(municipio: Municipio) {
        startActivity(
            Intent(requireContext(), GameActivity::class.java).apply {
                putExtra(GameActivity.EXTRA_MODE, mode.name)
                putExtra(GameActivity.EXTRA_MUNICIPIO, municipio.name)
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_MODE = "arg_mode"

        fun newInstance(mode: GameMode) = SelectMunicipioFragment().apply {
            arguments = Bundle().apply { putString(ARG_MODE, mode.name) }
        }
    }
}
