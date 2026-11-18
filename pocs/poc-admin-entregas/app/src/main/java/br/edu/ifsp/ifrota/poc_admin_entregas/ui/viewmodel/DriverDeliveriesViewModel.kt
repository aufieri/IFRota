package br.edu.ifsp.ifrota.poc_admin_entregas.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.edu.ifsp.ifrota.poc_admin_entregas.data.local.entity.DeliveryEntity
import br.edu.ifsp.ifrota.poc_admin_entregas.data.repository.DeliveryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Visão do motorista na PoC: a fila de entregas gravadas para ele, na ordem em que o
 * administrador as criou. No app principal esta mesma fila alimenta o `DeliveryViewModel`
 * e, a partir dele, o mapa e a navegação.
 */
class DriverDeliveriesViewModel(
    private val repository: DeliveryRepository
) : ViewModel() {

    /** `null` até o primeiro carregamento, para não mostrar "sem entregas" por engano. */
    val deliveries: StateFlow<List<DeliveryEntity>?> = repository.observeMyDeliveries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun complete(deliveryId: String) {
        viewModelScope.launch { repository.markDone(deliveryId) }
    }
}

class DriverDeliveriesViewModelFactory(
    private val repository: DeliveryRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(DriverDeliveriesViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return DriverDeliveriesViewModel(repository) as T
    }
}
