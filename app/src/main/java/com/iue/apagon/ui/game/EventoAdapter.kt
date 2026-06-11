package com.iue.apagon.ui.game

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.iue.apagon.R
import com.iue.apagon.databinding.ItemEventoBinding

/** Fila del reporte nocturno: un hecho de la noche (bueno o malo). */
data class ReporteEvento(
    val icon: String,
    val text: String,
    val bad: Boolean
)

class EventoAdapter(
    private val items: List<ReporteEvento>
) : RecyclerView.Adapter<EventoAdapter.VH>() {

    inner class VH(val binding: ItemEventoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemEventoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val e = items[position]
        val b = holder.binding
        val ctx = b.root.context
        val colorRes = if (e.bad) R.color.red else R.color.green
        val textColorRes = if (e.bad) R.color.red_light else R.color.green_light
        val tint = if (e.bad) 0x10EF4444 else 0x1022C55E

        b.eventoRoot.backgroundTintList = ColorStateList.valueOf(tint)
        b.eventoStrip.setBackgroundColor(ContextCompat.getColor(ctx, colorRes))
        b.eventoIcon.text = e.icon
        b.eventoText.text = e.text
        b.eventoText.setTextColor(ContextCompat.getColor(ctx, textColorRes))
    }
}
