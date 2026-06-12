package com.iue.apagon.ui.logros

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.iue.apagon.R
import com.iue.apagon.databinding.ItemLogroBinding
import com.iue.apagon.domain.engine.Logro

/** Grilla de logros: desbloqueados a color, bloqueados en gris con candado. */
class LogroAdapter : RecyclerView.Adapter<LogroAdapter.VH>() {

    /** Pares (logro, desbloqueado). */
    private var items: List<Pair<Logro, Boolean>> = emptyList()

    fun submit(items: List<Pair<Logro, Boolean>>) {
        this.items = items
        notifyDataSetChanged()
    }

    inner class VH(val binding: ItemLogroBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemLogroBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val (logro, desbloqueado) = items[position]
        val b = holder.binding
        val ctx = b.root.context

        b.logroTitulo.text = logro.titulo
        b.logroDescripcion.text = logro.descripcion

        if (desbloqueado) {
            b.logroRoot.alpha = 1f
            b.logroIcono.text = logro.icono
            b.logroTitulo.setTextColor(ContextCompat.getColor(ctx, R.color.text_strong))
            b.logroEstado.text = "+${logro.vatios} ⚡"
            b.logroEstado.setTextColor(ContextCompat.getColor(ctx, R.color.amber))
        } else {
            b.logroRoot.alpha = 0.5f
            b.logroIcono.text = "🔒"
            b.logroTitulo.setTextColor(ContextCompat.getColor(ctx, R.color.text_dim))
            b.logroEstado.text = "+${logro.vatios} ⚡"
            b.logroEstado.setTextColor(ContextCompat.getColor(ctx, R.color.text_dim))
        }
    }
}
