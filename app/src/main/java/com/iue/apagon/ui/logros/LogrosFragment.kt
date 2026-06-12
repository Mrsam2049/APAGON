package com.iue.apagon.ui.logros

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.iue.apagon.data.repository.GameRepository
import com.iue.apagon.databinding.FragmentLogrosBinding
import com.iue.apagon.domain.engine.Logro
import kotlinx.coroutines.launch

/**
 * Grilla de logros. Toma la lista completa del enum [Logro] y la cruza con el estado
 * persistido en Room (observado en vivo).
 */
class LogrosFragment : Fragment() {

    private var _binding: FragmentLogrosBinding? = null
    private val binding get() = _binding!!

    private val adapter = LogroAdapter()
    private val repo by lazy { GameRepository.get(requireContext().applicationContext) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogrosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnBack.setOnClickListener { requireActivity().finish() }
        binding.grid.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.grid.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                repo.ensureLogros()
                repo.observarLogros().collect { rows ->
                    val desbloqueados = rows.filter { it.desbloqueado }.map { it.id }.toSet()
                    val items = Logro.entries.map { it to (it.name in desbloqueados) }
                    adapter.submit(items)
                    binding.logrosProgreso.text = "${desbloqueados.size} / ${Logro.entries.size} desbloqueados"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
