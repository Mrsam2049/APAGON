package com.iue.apagon.domain.engine

/**
 * Logros del juego. Cada uno otorga Vatios al completarse. El [name] del enum se usa como id
 * persistente en Room (LogroEntity).
 */
enum class Logro(
    val titulo: String,
    val descripcion: String,
    val vatios: Int,
    val icono: String
) {
    PRIMER_APAGON("Primer Apagón", "Termina tu primera noche", 30, "🌙"),
    GUARDIAN_HOSPITAL("Guardián del Hospital", "Gana una partida sin apagar nunca el hospital", 80, "🏥"),
    CIEN_RENOVABLE("100% Renovable", "Gana usando solo solar, eólica e hidroeléctrica", 120, "🌱"),
    SUPERVIVIENTE("Superviviente", "Aguanta 7 noches en Supervivencia", 100, "🔥"),
    SACRIFICIO("Sacrificio", "Pierde un distrito rural", 40, "🌾"),
    ORO_PURO("Oro Puro", "Consigue un final Oro", 150, "🥇")
}
