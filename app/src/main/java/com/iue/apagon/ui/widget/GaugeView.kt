package com.iue.apagon.ui.widget

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.iue.apagon.R
import com.iue.apagon.databinding.ViewGaugeBinding

/**
 * Indicador tipo barra (cobertura / presupuesto / social / ambiental).
 * Reproduce el "Gauge" del prototipo: ícono + etiqueta + barra + porcentaje,
 * con color según el umbral (verde / ámbar / rojo).
 */
class GaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = ViewGaugeBinding.inflate(android.view.LayoutInflater.from(context), this)

    init {
        orientation = VERTICAL
        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.GaugeView)
            binding.gaugeIcon.text = a.getString(R.styleable.GaugeView_gaugeIcon) ?: "💡"
            binding.gaugeLabel.text = a.getString(R.styleable.GaugeView_gaugeLabel) ?: "—"
            a.recycle()
        }
    }

    fun setup(icon: String, label: String) {
        binding.gaugeIcon.text = icon
        binding.gaugeLabel.text = label
    }

    /** Actualiza el valor (0..100) y recolorea según criticidad. */
    fun setValue(value: Int) {
        val v = value.coerceIn(0, 100)
        val colorRes = when {
            v <= 25 -> R.color.red
            v <= 45 -> R.color.amber
            else -> R.color.green
        }
        val color = ContextCompat.getColor(context, colorRes)
        binding.gaugeBar.setIndicatorColor(color)
        binding.gaugeBar.setProgressCompat(v, true)
        binding.gaugeValue.text = context.getString(R.string.percent_fmt, v)
        binding.gaugeValue.setTextColor(color)
    }

    @Suppress("unused")
    private fun parseColor(hex: String): Int = Color.parseColor(hex)
}
