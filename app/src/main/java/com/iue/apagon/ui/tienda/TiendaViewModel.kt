package com.iue.apagon.ui.tienda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.iue.apagon.data.local.entity.PerfilEntity
import com.iue.apagon.data.repository.GameRepository
import com.iue.apagon.domain.engine.Unlockable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Resultado de un intento de compra, para feedback inmediato (toast/animación). */
sealed class CompraResult {
    data class Ok(val item: Unlockable) : CompraResult()
    data class Fail(val item: Unlockable, val motivo: String?) : CompraResult()
}

/**
 * ViewModel del Centro de Energía (tienda). Observa el perfil (Flow) para refrescar Vatios y
 * lo desbloqueado en tiempo real, y ejecuta las compras.
 */
class TiendaViewModel(private val repo: GameRepository) : ViewModel() {

    val perfil: StateFlow<PerfilEntity?> =
        repo.observarPerfil().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _eventos = MutableSharedFlow<CompraResult>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    init {
        // Garantiza que exista la fila de perfil para que el Flow emita.
        viewModelScope.launch { repo.perfil() }
    }

    fun comprar(item: Unlockable) {
        viewModelScope.launch {
            val r = repo.desbloquear(item)
            _eventos.emit(
                if (r.isSuccess) CompraResult.Ok(item)
                else CompraResult.Fail(item, r.exceptionOrNull()?.message)
            )
        }
    }

    companion object {
        fun factory(repo: GameRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { TiendaViewModel(repo) }
        }
    }
}
