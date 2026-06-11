package com.iue.apagon.domain.model

enum class GameMode {
    CAMPANA,        // 5 noches
    SUPERVIVENCIA   // infinito, intensidad El Niño creciente
}

enum class Municipio(val displayName: String) {
    APARTADO("Apartadó"),
    QUIBDO("Quibdó"),
    RIOHACHA("Riohacha")
}

/**
 * Motivos de derrota reconocidos por el GameEngine.
 * (El presupuesto NO es condición de derrota según las reglas del juego.)
 */
enum class GameOverReason {
    HOSPITAL,    // hospital apagado 2 noches seguidas
    BIENESTAR,   // bienestar social <= 0
    COBERTURA,   // cobertura == 0
    AMBIENTAL    // índice ambiental <= 0
}
