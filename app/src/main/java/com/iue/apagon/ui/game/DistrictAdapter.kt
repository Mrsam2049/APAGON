package com.iue.apagon.ui.game

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.iue.apagon.R
import com.iue.apagon.databinding.ItemDistrictBinding
import com.iue.apagon.domain.model.District
import com.iue.apagon.ui.Presentation

/**
 * Grid 2×2 de distritos. Cada celda muestra estado (ON/OFF/PERDIDO), ícono, nombre, regla
 * y demanda; el fondo cambia según el estado. Tocar alterna encendido/apagado.
 */
class DistrictAdapter(
    private val onToggle: (String) -> Unit
) : RecyclerView.Adapter<DistrictAdapter.VH>() {

    private var items: List<District> = emptyList()

    fun submit(districts: List<District>) {
        items = districts
        notifyDataSetChanged()
    }

    inner class VH(val binding: ItemDistrictBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemDistrictBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val d = items[position]
        val b = holder.binding
        val ctx = b.root.context
        val hot = d.demand > d.baseDemand

        b.districtRoot.setBackgroundResource(
            when {
                d.lost -> R.drawable.district_lost
                !d.on -> R.drawable.district_off
                hot -> R.drawable.district_on_hot
                else -> R.drawable.district_on
            }
        )
        b.districtRoot.alpha = if (d.lost) 0.4f else 1f
        b.districtRoot.isEnabled = !d.lost

        b.statusEmoji.text = when {
            d.lost -> "❌"
            d.on -> "💡"
            else -> "🌑"
        }
        b.statusLabel.text = when {
            d.lost -> "PERDIDO"
            d.on -> "ON"
            else -> "OFF"
        }
        b.statusLabel.setTextColor(
            ContextCompat.getColor(
                ctx,
                when {
                    d.lost -> R.color.text_faint
                    d.on -> R.color.green
                    else -> R.color.red
                }
            )
        )

        b.districtIcon.text = Presentation.traitIcon(d.trait)
        b.districtName.text = d.name
        b.districtName.setTextColor(
            ContextCompat.getColor(ctx, if (d.on && !d.lost) R.color.text_strong else R.color.text_dim)
        )
        b.districtRule.text = Presentation.traitRule(d.trait)

        b.districtDemand.text = if (hot) "${d.demand} MW 🔥" else "${d.demand} MW"
        b.districtDemand.setTextColor(
            ContextCompat.getColor(
                ctx,
                when {
                    hot -> R.color.amber
                    d.on && !d.lost -> R.color.blue_light
                    else -> R.color.text_faint
                }
            )
        )

        b.districtRoot.setOnClickListener { if (!d.lost) onToggle(d.id) }
    }
}
