package com.iue.apagon.domain.engine

import com.iue.apagon.domain.model.District
import com.iue.apagon.domain.model.DistrictTrait
import com.iue.apagon.domain.model.Municipio

/**
 * Distritos base de cada municipio. [districtsFor] devuelve una lista nueva (estado fresco)
 * lista para arrancar una partida. Los ids de Apartadó coinciden con los modificadores de
 * demanda de [DayScenarios] (centro, norte, bananera, vereda).
 */
object MunicipioData {

    fun districtsFor(municipio: Municipio): List<District> = when (municipio) {
        Municipio.APARTADO -> apartado()
        Municipio.QUIBDO -> quibdo()
        Municipio.RIOHACHA -> riohacha()
    }

    private fun apartado(): List<District> = listOf(
        District(id = "centro", name = "Centro", trait = DistrictTrait.HOSPITAL, demand = 26),
        District(id = "norte", name = "Norte", trait = DistrictTrait.RESIDENCIAL, demand = 20),
        District(id = "bananera", name = "Zona Bananera", trait = DistrictTrait.INDUSTRIAL, demand = 30),
        District(id = "vereda", name = "La Vereda", trait = DistrictTrait.RURAL, demand = 16)
    )

    private fun quibdo(): List<District> = listOf(
        District(id = "hospital_sf", name = "Hospital San Francisco", trait = DistrictTrait.HOSPITAL, demand = 24),
        District(id = "nino_jesus", name = "Niño Jesús", trait = DistrictTrait.RESIDENCIAL, demand = 22),
        District(id = "puerto_fluvial", name = "Puerto Fluvial", trait = DistrictTrait.INDUSTRIAL, demand = 28),
        District(id = "zona_rural", name = "Zona Rural", trait = DistrictTrait.RURAL, demand = 18)
    )

    private fun riohacha(): List<District> = listOf(
        District(id = "hospital_ns", name = "Hospital Nuestra Señora", trait = DistrictTrait.HOSPITAL, demand = 25),
        District(id = "zona_turistica", name = "Zona Turística", trait = DistrictTrait.RESIDENCIAL, demand = 23),
        District(id = "puerto_maritimo", name = "Puerto Marítimo", trait = DistrictTrait.INDUSTRIAL, demand = 29),
        District(id = "rancheria_wayuu", name = "Ranchería Wayúu", trait = DistrictTrait.RURAL, demand = 15)
    )
}
