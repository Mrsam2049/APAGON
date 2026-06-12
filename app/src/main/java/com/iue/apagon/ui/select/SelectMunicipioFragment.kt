package com.iue.apagon.ui.select

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.iue.apagon.data.repository.GameRepository
import com.iue.apagon.databinding.FragmentSelectMunicipioBinding
import com.iue.apagon.domain.model.GameMode
import com.iue.apagon.domain.model.Municipio
import com.iue.apagon.ui.game.GameActivity
import com.iue.apagon.ui.tienda.TiendaActivity
import kotlinx.coroutines.launch

/**
 * Selección de municipio (HU-01): un RecyclerView con las 3 ciudades. Los no desbloqueados
 * salen con candado (y redirigen al Centro de Energía). Al elegir uno disponible, lanza
 * GameActivity con el modo + municipio.
 */
class SelectMunicipioFragment : Fragment() {

    private var _binding: FragmentSelectMunicipioBinding? = null
    private val binding get() = _binding!!

    private val repo by lazy { GameRepository.get(requireContext().applicationContext) }
    private lateinit var adapter: CityAdapter

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

        adapter = CityAdapter(
            onClick = { municipio -> launchGame(municipio) },
            onLocked = { municipio ->
                Toast.makeText(
                    requireContext(),
                    "🔒 Desbloquea ${municipio.displayName} en el Centro de Energía",
                    Toast.LENGTH_SHORT
                ).show()
                startActivity(Intent(requireContext(), TiendaActivity::class.java))
            }
        )
        binding.cityList.layoutManager = LinearLayoutManager(requireContext())
        binding.cityList.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                repo.perfil() // asegura la fila por defecto
                repo.observarPerfil().collect { perfil ->
                    adapter.setUnlocked(csv(perfil?.municipiosDesbloqueados))
                }
            }
        }
    }

    private fun launchGame(municipio: Municipio) {
        startActivity(
            Intent(requireContext(), GameActivity::class.java).apply {
                putExtra(GameActivity.EXTRA_MODE, mode.name)
                putExtra(GameActivity.EXTRA_MUNICIPIO, municipio.name)
            }
        )
    }

    private fun csv(value: String?): Set<String> =
        value?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet() ?: setOf("apartado")

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
