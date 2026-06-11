package com.iue.apagon.ui.game

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.iue.apagon.R
import com.iue.apagon.databinding.ItemCardBinding
import com.iue.apagon.domain.model.Card
import com.iue.apagon.ui.Presentation

/**
 * Mano de cartas (lista horizontal). Una carta bloqueada (sin puntos o sin presupuesto)
 * se atenúa. Tocar una jugable la juega.
 */
class CardAdapter(
    private val onPlay: (Card) -> Unit
) : RecyclerView.Adapter<CardAdapter.VH>() {

    private var items: List<Card> = emptyList()
    private var actionPoints: Int = 2
    private var budget: Int = 0

    fun submit(cards: List<Card>, actionPoints: Int, budget: Int) {
        this.items = cards
        this.actionPoints = actionPoints
        this.budget = budget
        notifyDataSetChanged()
    }

    inner class VH(val binding: ItemCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val card = items[position]
        val b = holder.binding
        val ctx = b.root.context
        val blocked = actionPoints <= 0 || card.cost > budget
        val accent = Color.parseColor(Presentation.cardAccent(card))

        b.cardIcon.text = Presentation.cardIcon(card)
        b.cardName.text = card.name
        b.cardDesc.text = card.description
        b.cardCost.text = if (card.cost == 0) "Gratis" else "$${card.cost}M"
        b.cardCost.setTextColor(
            ContextCompat.getColor(ctx, if (card.cost == 0) R.color.green else R.color.blue_light)
        )

        // Borde con el color de la carta (conserva el gradiente del drawable).
        val density = ctx.resources.displayMetrics.density
        (ContextCompat.getDrawable(ctx, R.drawable.card_panel)?.mutate() as? GradientDrawable)?.let { bg ->
            bg.setStroke((1 * density).toInt(), (0x66000000.toInt() or (accent and 0x00FFFFFF)))
            b.cardRoot.background = bg
        }

        b.cardRoot.alpha = if (blocked) 0.4f else 1f
        b.cardRoot.isEnabled = !blocked
        b.cardRoot.setOnClickListener { if (!blocked) onPlay(card) }
    }
}
