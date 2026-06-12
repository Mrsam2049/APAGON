package com.iue.apagon.domain.engine

/** Categoría de un desbloqueable. */
enum class UnlockKind { MUNICIPIO, MEJORA, CARTA }

/**
 * Ítem comprable en el Centro de Energía. Datos puros (sin Android), para que tanto el engine
 * como la UI consulten la misma tabla de costos.
 */
data class Unlockable(
    val id: String,
    val kind: UnlockKind,
    val nombre: String,
    val descripcion: String,
    val costo: Int
)

/** Catálogo/economía de desbloqueos. */
object Unlockables {

    val municipios: List<Unlockable> = listOf(
        Unlockable("quibdo", UnlockKind.MUNICIPIO, "Quibdó", "Chocó · 4 distritos propios", 300),
        Unlockable("riohacha", UnlockKind.MUNICIPIO, "Riohacha", "La Guajira · 4 distritos propios", 600)
    )

    val mejoras: List<Unlockable> = listOf(
        Unlockable("presupuesto_inicial", UnlockKind.MEJORA, "Presupuesto inicial", "+10% de presupuesto al iniciar", 250),
        Unlockable("cuadrilla_extra", UnlockKind.MEJORA, "Cuadrilla extra", "Empieza con 3 jugadas por noche", 500),
        Unlockable("colchon_social", UnlockKind.MEJORA, "Colchón social", "Empieza con +10 de bienestar social", 400),
        Unlockable("energia_base", UnlockKind.MEJORA, "Energía base", "+5 MW disponibles cada noche", 450)
    )

    val cartas: List<Unlockable> = listOf(
        Unlockable("hidroelectrica", UnlockKind.CARTA, "Hidroeléctrica", "+30 MW · \$8M · +4 ambiental", 350),
        Unlockable("eficiencia", UnlockKind.CARTA, "Eficiencia Energética", "Reduce 30% la demanda · gratis", 300),
        Unlockable("subsidio", UnlockKind.CARTA, "Subsidio Estatal", "+\$15M de presupuesto · gratis", 400)
    )

    val all: List<Unlockable> = municipios + mejoras + cartas

    fun byId(id: String): Unlockable? = all.firstOrNull { it.id == id }
}
