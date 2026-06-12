package com.iue.apagon.ui.tienda

import android.animation.ValueAnimator
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.iue.apagon.data.repository.GameRepository
import com.iue.apagon.databinding.ActivityTiendaBinding
import com.iue.apagon.domain.engine.Unlockable
import com.iue.apagon.domain.engine.Unlockables
import kotlinx.coroutines.launch

/**
 * Centro de Energía: tabs Municipios / Mejoras / Cartas. El saldo de Vatios y lo desbloqueado
 * se refrescan en vivo desde el perfil (Room → Flow).
 */
class TiendaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTiendaBinding

    private val viewModel: TiendaViewModel by viewModels {
        TiendaViewModel.factory(GameRepository.get(applicationContext))
    }

    private val adapter = UnlockableAdapter { item -> viewModel.comprar(item) }

    private var tabIndex = 0
    private var vatiosMostrados = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTiendaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.lista.layoutManager = LinearLayoutManager(this)
        binding.lista.adapter = adapter

        listOf("Municipios", "Mejoras", "Cartas").forEach {
            binding.tabs.addTab(binding.tabs.newTab().setText(it))
        }
        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                tabIndex = tab.position
                render()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        observe()
    }

    private fun observe() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.perfil.collect { render() } }
                launch {
                    viewModel.eventos.collect { ev ->
                        when (ev) {
                            is CompraResult.Ok ->
                                Toast.makeText(this@TiendaActivity, "✅ ${ev.item.nombre} desbloqueado", Toast.LENGTH_SHORT).show()
                            is CompraResult.Fail ->
                                Toast.makeText(this@TiendaActivity, "No se pudo: ${ev.motivo ?: "error"}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun render() {
        val perfil = viewModel.perfil.value
        val vatios = perfil?.vatiosTotales ?: 0
        animarVatios(vatios)

        val owned: Set<String> = when (tabIndex) {
            0 -> csv(perfil?.municipiosDesbloqueados)
            1 -> csv(perfil?.mejorasCompradas)
            else -> csv(perfil?.cartasDesbloqueadas)
        }
        val items: List<Unlockable> = when (tabIndex) {
            0 -> Unlockables.municipios
            1 -> Unlockables.mejoras
            else -> Unlockables.cartas
        }
        adapter.submit(items, owned, vatios)
    }

    /** Cuenta animada del saldo de Vatios en la cabecera. */
    private fun animarVatios(destino: Int) {
        if (destino == vatiosMostrados) {
            binding.vatiosTotales.text = destino.toString()
            return
        }
        ValueAnimator.ofInt(vatiosMostrados, destino).apply {
            duration = 500
            addUpdateListener { binding.vatiosTotales.text = (it.animatedValue as Int).toString() }
        }.start()
        vatiosMostrados = destino
    }

    private fun csv(value: String?): Set<String> =
        value?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()
}
