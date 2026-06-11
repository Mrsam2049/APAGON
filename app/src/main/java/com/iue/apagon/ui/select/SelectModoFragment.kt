package com.iue.apagon.ui.select

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.iue.apagon.databinding.FragmentSelectModoBinding
import com.iue.apagon.domain.model.GameMode

/**
 * Pantalla de menú: elige modo de juego. Al elegir, navega a la selección de municipio
 * pasando el modo. Equivale a la fase "menu" del prototipo.
 */
class SelectModoFragment : Fragment() {

    private var _binding: FragmentSelectModoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectModoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.cardCampana.setOnClickListener { goToMunicipio(GameMode.CAMPANA) }
        binding.cardSurvival.setOnClickListener { goToMunicipio(GameMode.SUPERVIVENCIA) }
    }

    private fun goToMunicipio(mode: GameMode) {
        parentFragmentManager.beginTransaction()
            .replace(
                (requireView().parent as ViewGroup).id,
                SelectMunicipioFragment.newInstance(mode)
            )
            .addToBackStack("municipio")
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
