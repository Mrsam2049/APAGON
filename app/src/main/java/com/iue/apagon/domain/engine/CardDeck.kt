package com.iue.apagon.domain.engine

import com.iue.apagon.domain.model.Card
import com.iue.apagon.domain.model.CardType
import com.iue.apagon.domain.model.EnergySource
import kotlin.random.Random

/**
 * Pool de cartas del juego y reparto de manos.
 *
 * [drawWeighted] reparte n cartas según su peso (las básicas y gratuitas salen más a menudo)
 * y garantiza que la mano siempre tenga al menos una carta de tipo ENERGIA, para que el
 * jugador nunca quede sin forma de generar energía. A cada carta repartida se le asigna un
 * id único (sufijo por posición) para poder distinguir duplicados en la mano.
 */
object CardDeck {

    // ── Plantillas de cartas (id base; drawWeighted las clona con id único) ──
    private val solar = Card(
        id = "solar", name = "Paneles Solares", type = CardType.ENERGIA,
        description = "Energía limpia: +14 MW y mejora el índice ambiental.",
        mw = 14, source = EnergySource.SOLAR, cost = 0
    )
    private val eolica = Card(
        id = "eolica", name = "Parque Eólico", type = CardType.ENERGIA,
        description = "Mucha energía limpia: +26 MW.",
        mw = 26, source = EnergySource.EOLICA, cost = 10
    )
    private val termica = Card(
        id = "termica", name = "Planta Térmica", type = CardType.ENERGIA,
        description = "Potencia bruta: +34 MW, pero contamina.",
        mw = 34, source = EnergySource.TERMICA, cost = 6
    )
    private val reparacion = Card(
        id = "reparacion", name = "Cuadrilla de Reparación", type = CardType.ENERGIA,
        description = "Recupera capacidad de la red: +20 MW.",
        mw = 20, source = EnergySource.REPARACION, cost = 0
    )
    private val baterias = Card(
        id = "baterias", name = "Banco de Baterías", type = CardType.ENERGIA,
        description = "Descarga la energía almacenada: +16 MW.",
        mw = 16, source = EnergySource.BATERIA, cost = 4
    )
    private val campanaCiudadana = Card(
        id = "campana_ciudadana", name = "Campaña Ciudadana", type = CardType.CAMPANA_CIUDADANA,
        description = "Sensibiliza antes de apagar: -60% de penalización social esta noche.",
        cost = 0
    )
    private val racionamiento = Card(
        id = "racionamiento", name = "Racionamiento Programado", type = CardType.RACIONAMIENTO,
        description = "Reduce la demanda de todos los distritos esta noche.",
        cost = 0
    )

    /** Todas las cartas disponibles. */
    val pool: List<Card> = listOf(
        solar, eolica, termica, reparacion, baterias, campanaCiudadana, racionamiento
    )

    /** Carta -> peso de aparición. Las gratuitas/básicas pesan más. */
    private val weighted: List<Pair<Card, Int>> = listOf(
        solar to 4,
        reparacion to 3,
        baterias to 3,
        termica to 2,
        eolica to 2,
        campanaCiudadana to 3,
        racionamiento to 3
    )

    private val energyCards: List<Card> = pool.filter { it.type == CardType.ENERGIA }

    /**
     * Reparte [n] cartas con peso, garantizando al menos una de tipo ENERGIA.
     */
    fun drawWeighted(n: Int, rng: Random = Random.Default): List<Card> {
        if (n <= 0) return emptyList()

        val drawn = ArrayList<Card>(n)
        repeat(n) { drawn.add(pickWeighted(rng)) }

        // Garantía: si no salió ninguna ENERGIA, reemplazamos una al azar por una ENERGIA.
        if (drawn.none { it.type == CardType.ENERGIA }) {
            drawn[rng.nextInt(drawn.size)] = energyCards.random(rng)
        }

        // Id único por posición para distinguir duplicados en la mano.
        return drawn.mapIndexed { index, card -> card.copy(id = "${card.id}_$index") }
    }

    private fun pickWeighted(rng: Random): Card {
        val totalWeight = weighted.sumOf { it.second }
        var roll = rng.nextInt(totalWeight)
        for ((card, weight) in weighted) {
            if (roll < weight) return card
            roll -= weight
        }
        return weighted.last().first
    }
}
