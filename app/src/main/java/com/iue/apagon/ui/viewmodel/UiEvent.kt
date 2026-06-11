package com.iue.apagon.ui.viewmodel

/**
 * Eventos de una sola vez (one-shot), emitidos por un SharedFlow para que no se vuelvan a
 * disparar al recrear la vista (rotación). La UI los consume como Toast/Snackbar.
 */
sealed class UiEvent {
    /** Intento de encender un distrito o cerrar la noche con la red sobrecargada. */
    data object OverloadError : UiEvent()

    /** Carta no jugable: sin puntos de acción o presupuesto insuficiente. */
    data object CardBlocked : UiEvent()
}
