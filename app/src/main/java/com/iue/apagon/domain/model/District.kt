package com.iue.apagon.domain.model

/**
 * Personalidad de cada distrito. Define cómo reacciona ante apagones.
 */
enum class DistrictTrait {
    HOSPITAL,     // no apagar 2 noches seguidas o game over
    INDUSTRIAL,   // da +6M encendido, penaliza apagado
    RESIDENCIAL,  // descontento acumulado por noches consecutivas apagado
    RURAL         // si totalOff >= 3 se pierde para siempre
}

/**
 * Distrito dentro de un municipio. Combina datos fijos (id, nombre, trait, baseDemand)
 * con estado de runtime que el GameEngine actualiza noche a noche.
 *
 * - [on]: encendido esta noche (lo decide el jugador).
 * - [lost]: perdido de forma permanente; deja de contar para la cobertura.
 * - [consecutiveOff]: noches consecutivas apagado (se resetea al encender).
 * - [totalOff]: total acumulado de noches apagado.
 * - [demand]: demanda actual (puede reducirse temporalmente por RACIONAMIENTO).
 * - [baseDemand]: demanda original; el engine la restaura al iniciar cada noche.
 */
data class District(
    val id: String,
    val name: String,
    val trait: DistrictTrait,
    val demand: Int,
    val baseDemand: Int = demand,
    val on: Boolean = true,
    val lost: Boolean = false,
    val consecutiveOff: Int = 0,
    val totalOff: Int = 0
)
