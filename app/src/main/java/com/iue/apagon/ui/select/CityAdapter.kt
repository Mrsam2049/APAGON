package com.iue.apagon.ui.select

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.iue.apagon.R
import com.iue.apagon.databinding.ItemCityBinding
import com.iue.apagon.databinding.ItemCityMiniBinding
import com.iue.apagon.domain.engine.MunicipioData
import com.iue.apagon.domain.engine.Unlockables
import com.iue.apagon.domain.model.Municipio
import com.iue.apagon.ui.Presentation

/**
 * Lista de municipios seleccionables. Muestra cobertura y un mini-resumen de los 4 distritos.
 * Los municipios no desbloqueados aparecen atenuados con un candado y su costo.
 */
class CityAdapter(
    private val onClick: (Municipio) -> Unit,
    private val onLocked: (Municipio) -> Unit
) : RecyclerView.Adapter<CityAdapter.VH>() {

    private val cities = Municipio.entries.toList()
    private var unlocked: Set<String> = setOf("apartado")

    fun setUnlocked(set: Set<String>) {
        unlocked = if (set.isEmpty()) setOf("apartado") else set
        notifyDataSetChanged()
    }

    inner class VH(val binding: ItemCityBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = cities.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val municipio = cities[position]
        val b = holder.binding
        val ctx = b.root.context

        b.cityName.text = municipio.displayName
        b.cityDept.text = Presentation.dept(municipio)

        val cobertura = Presentation.cobertura(municipio)
        b.cityCobertura.text = ctx.getString(R.string.percent_fmt, cobertura)
        val coverColor = if (cobertura > 75) R.color.green else R.color.amber
        b.cityCobertura.setTextColor(ContextCompat.getColor(ctx, coverColor))

        // Mini-tiles de distritos.
        b.districtsRow.removeAllViews()
        val districts = MunicipioData.districtsFor(municipio)
        districts.forEachIndexed { index, d ->
            val mini = ItemCityMiniBinding.inflate(LayoutInflater.from(ctx), b.districtsRow, false)
            mini.miniIcon.text = Presentation.traitIcon(d.trait)
            mini.miniName.text = d.name.substringBefore(" ")
            if (index == districts.lastIndex) {
                (mini.root.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 0
            }
            b.districtsRow.addView(mini.root)
        }

        // Estado de bloqueo.
        val id = municipio.name.lowercase()
        val locked = id !in unlocked
        if (locked) {
            b.root.alpha = 0.55f
            b.lockBadge.visibility = View.VISIBLE
            val costo = Unlockables.byId(id)?.costo ?: 0
            b.lockBadge.text = "🔒 $costo ⚡ en el Centro"
            b.root.setOnClickListener { onLocked(municipio) }
        } else {
            b.root.alpha = 1f
            b.lockBadge.visibility = View.GONE
            b.root.setOnClickListener { onClick(municipio) }
        }
    }
}
