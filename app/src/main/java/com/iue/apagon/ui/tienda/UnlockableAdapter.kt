package com.iue.apagon.ui.tienda

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.iue.apagon.R
import com.iue.apagon.databinding.ItemUnlockableBinding
import com.iue.apagon.domain.engine.Unlockable

/**
 * Lista de ítems comprables (municipios / mejoras / cartas). El botón cambia según si ya está
 * desbloqueado, si alcanza el saldo, o si faltan Vatios.
 */
class UnlockableAdapter(
    private val onBuy: (Unlockable) -> Unit
) : RecyclerView.Adapter<UnlockableAdapter.VH>() {

    private var items: List<Unlockable> = emptyList()
    private var owned: Set<String> = emptySet()
    private var vatios: Int = 0

    fun submit(items: List<Unlockable>, owned: Set<String>, vatios: Int) {
        this.items = items
        this.owned = owned
        this.vatios = vatios
        notifyDataSetChanged()
    }

    inner class VH(val binding: ItemUnlockableBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemUnlockableBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val b = holder.binding
        val ctx = b.root.context

        b.itemNombre.text = item.nombre
        b.itemDescripcion.text = item.descripcion
        b.itemCosto.text = "${item.costo} ⚡"

        val esDueno = item.id in owned
        val alcanza = vatios >= item.costo
        val boton = b.itemBoton

        when {
            esDueno -> {
                boton.text = "✓ Desbloqueado"
                boton.setBackgroundResource(R.drawable.btn_disabled)
                boton.setTextColor(ContextCompat.getColor(ctx, R.color.green))
                boton.isClickable = false
                b.itemCosto.setTextColor(ContextCompat.getColor(ctx, R.color.green))
            }
            alcanza -> {
                boton.text = "Desbloquear"
                boton.setBackgroundResource(R.drawable.btn_primary)
                boton.setTextColor(0xFFFFFFFF.toInt())
                boton.isClickable = true
                boton.setOnClickListener { onBuy(item) }
                b.itemCosto.setTextColor(ContextCompat.getColor(ctx, R.color.amber))
            }
            else -> {
                boton.text = "Faltan ${item.costo - vatios} ⚡"
                boton.setBackgroundResource(R.drawable.btn_disabled)
                boton.setTextColor(ContextCompat.getColor(ctx, R.color.text_dim))
                boton.isClickable = false
                b.itemCosto.setTextColor(ContextCompat.getColor(ctx, R.color.text_dim))
            }
        }
    }
}
