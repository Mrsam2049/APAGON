package com.iue.apagon.ui.game

import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.iue.apagon.R
import com.iue.apagon.domain.engine.Logro
import com.iue.apagon.domain.engine.VatiosBreakdown

/** Helpers compartidos por las pantallas de victoria y derrota (bloque de Vatios + badges). */
object EndScreenUi {

    /** Contador animado de "+XXX ⚡" con su desglose. */
    fun animateVatios(totalView: TextView, desgloseView: TextView, b: VatiosBreakdown) {
        val sb = StringBuilder()
        sb.append("indicadores +${b.indicadores} · noches +${b.noches} · medalla +${b.medalla}")
        if (b.supervivencia > 0) sb.append(" · supervivencia +${b.supervivencia}")
        desgloseView.text = sb.toString()

        ValueAnimator.ofInt(0, b.total).apply {
            duration = 900L
            addUpdateListener { totalView.text = "+${it.animatedValue as Int} ⚡" }
        }.start()
    }

    /** Pinta un badge por cada logro recién desbloqueado en [container]. */
    fun renderBadges(container: LinearLayout, logros: List<Logro>) {
        container.removeAllViews()
        if (logros.isEmpty()) return
        val ctx = container.context
        val d = ctx.resources.displayMetrics.density

        logros.forEach { logro ->
            val tv = TextView(ctx).apply {
                text = "${logro.icono} Logro: ${logro.titulo}  +${logro.vatios} ⚡"
                setTextColor(ContextCompat.getColor(ctx, R.color.green_light))
                textSize = 11f
                setTypeface(typeface, Typeface.BOLD)
                setBackgroundResource(R.drawable.rounded_8)
                backgroundTintList = ColorStateList.valueOf(0x2222C55E)
                val px = (10 * d).toInt()
                val py = (8 * d).toInt()
                setPadding(px, py, px, py)
            }
            tv.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (6 * d).toInt() }
            container.addView(tv)
        }
    }
}
