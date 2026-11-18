package br.edu.ifsp.ifrota.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.edu.ifsp.ifrota.data.admin.AdminDriver
import br.edu.ifsp.ifrota.data.admin.AdminGateway
import br.edu.ifsp.ifrota.data.admin.CreateDeliveriesResult
import br.edu.ifsp.ifrota.data.delivery.DemoDeliveryGenerator
import br.edu.ifsp.ifrota.data.local.entity.DeliveryEntity
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Lista de motoristas: [Loading] até o primeiro resultado do servidor. */
sealed interface DriversState {
    data object Loading : DriversState
    data class Ready(val drivers: List<AdminDriver>) : DriversState
    data class Error(val message: String) : DriversState
}

data class CreateDeliveriesUiState(
    val quantity: Int = DemoDeliveryGenerator.DEFAULT_QUANTITY,
    val isCreating: Boolean = false,
    /** Resultado mais recente, mostrado uma vez na tela. */
    val message: AdminMessage? = null
)

data class AdminMessage(val text: String, val isError: Boolean)

class AdminViewModel(
    private val repository: AdminGateway
) : ViewModel() {

    val drivers: StateFlow<DriversState> = repository.observeDrivers()
        .map<List<AdminDriver>, DriversState> { DriversState.Ready(it) }
        .catch { emit(DriversState.Error("Não foi possível carregar os motoristas. Verifique a conexão e as permissões da conta.")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DriversState.Loading)

    private val _create = MutableStateFlow(CreateDeliveriesUiState())
    val create: StateFlow<CreateDeliveriesUiState> = _create.asStateFlow()

    /**
     * Identifica a ação em andamento do administrador. Só muda depois de um sucesso: uma nova
     * tentativa após falha de rede reaproveita o mesmo id e, por isso, nunca duplica entregas.
     */
    private var batchId = UUID.randomUUID().toString()
    private var batchDriverId: String? = null

    fun deliveriesOf(driverId: String): Flow<List<DeliveryEntity>> = flow {
        emitAll(repository.observeDeliveries(driverId).catch { emit(emptyList()) })
    }

    fun onQuantityChange(value: Int) =
        _create.update { it.copy(quantity = DemoDeliveryGenerator.clampQuantity(value)) }

    fun consumeMessage() = _create.update { it.copy(message = null) }

    /** Abrir o formulário para outro motorista começa uma ação nova. */
    fun startCreating(driverId: String) {
        if (batchDriverId != driverId) {
            batchDriverId = driverId
            batchId = UUID.randomUUID().toString()
        }
        _create.update { it.copy(message = null) }
    }

    fun createDeliveries(driverId: String) {
        if (_create.value.isCreating) return
        _create.update { it.copy(isCreating = true, message = null) }
        val quantity = _create.value.quantity
        val currentBatch = batchId

        viewModelScope.launch {
            val result = repository.createDeliveries(driverId, currentBatch, quantity)
            val message = when (result) {
                is CreateDeliveriesResult.Created -> {
                    batchId = UUID.randomUUID().toString()
                    AdminMessage(
                        if (result.count == 1) "1 entrega criada para o motorista." else "${result.count} entregas criadas para o motorista.",
                        isError = false
                    )
                }
                is CreateDeliveriesResult.AlreadyCreated -> {
                    batchId = UUID.randomUUID().toString()
                    AdminMessage("Essas entregas já tinham sido criadas. Nada foi duplicado.", isError = false)
                }
                CreateDeliveriesResult.DriverNotFound ->
                    AdminMessage("Este motorista não está mais disponível.", isError = true)
                CreateDeliveriesResult.NoAddressesAvailable ->
                    AdminMessage(
                        "Todos os endereços de demonstração já estão pendentes para este motorista. Conclua algumas entregas antes de criar outras.",
                        isError = true
                    )
                is CreateDeliveriesResult.Failure -> AdminMessage(result.message, isError = true)
            }
            _create.update { it.copy(isCreating = false, message = message) }
        }
    }
}

class AdminViewModelFactory(
    private val repository: AdminGateway
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AdminViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return AdminViewModel(repository) as T
    }
}
