package com.iue.apagon.ui

import com.iue.apagon.domain.engine.NightScenario
import com.iue.apagon.domain.model.Card
import com.iue.apagon.domain.model.CardType
import com.iue.apagon.domain.model.DistrictTrait
import com.iue.apagon.domain.model.EnergySource
import com.iue.apagon.domain.model.Municipio

/**
 * Datos de presentación que no viven en el dominio (íconos, reglas legibles, metadatos de
 * ciudad). Mantiene la UI fiel al prototipo sin contaminar el GameEngine.
 */
object Presentation {

    fun traitIcon(trait: DistrictTrait): String = when (trait) {
        DistrictTrait.HOSPITAL -> "🏥"
        DistrictTrait.RESIDENCIAL -> "🏘️"
        DistrictTrait.INDUSTRIAL -> "🏭"
        DistrictTrait.RURAL -> "🌾"
    }

    fun traitRule(trait: DistrictTrait): String = when (trait) {
        DistrictTrait.HOSPITAL -> "No apagar 2 noches seguidas"
        DistrictTrait.RESIDENCIAL -> "El descontento se acumula"
        DistrictTrait.INDUSTRIAL -> "Da +\$6M encendido; cuesta apagarlo"
        DistrictTrait.RURAL -> "Si la abandonas, la pierdes"
    }

    fun dept(municipio: Municipio): String = when (municipio) {
        Municipio.APARTADO -> "Antioquia"
        Municipio.QUIBDO -> "Chocó"
        Municipio.RIOHACHA -> "La Guajira"
    }

    /** Cobertura "real" mostrada en la selección de municipio (datos XM del prototipo). */
    fun cobertura(municipio: Municipio): Int = when (municipio) {
        Municipio.APARTADO -> 87
        Municipio.QUIBDO -> 62
        Municipio.RIOHACHA -> 71
    }

    /** Color hex de acento según el escenario (para fondos/títulos). */
    fun scenarioColor(scenario: NightScenario): String = when (scenario.id) {
        "sequia_critica", "falla_subestacion" -> "#EF4444"
        "ola_de_calor" -> "#F59E0B"
        "festival" -> "#A855F7"
        else -> "#3B82F6"
    }

    /** Ícono de la carta según su tipo/fuente. */
    fun cardIcon(card: Card): String = when (card.type) {
        CardType.CAMPANA_CIUDADANA -> "📢"
        CardType.RACIONAMIENTO -> "🪫"
        CardType.ENERGIA -> when (card.source) {
            EnergySource.SOLAR -> "☀️"
            EnergySource.EOLICA -> "💨"
            EnergySource.TERMICA -> "🏭"
            EnergySource.REPARACION -> "🔧"
            EnergySource.BATERIA -> "🔋"
            null -> "⚡"
        }
    }

    /** Color hex de acento de la carta (borde / costo). */
    fun cardAccent(card: Card): String = when (card.type) {
        CardType.ENERGIA -> "#22C55E"
        CardType.CAMPANA_CIUDADANA -> "#3B82F6"
        CardType.RACIONAMIENTO -> "#F59E0B"
    }
}
