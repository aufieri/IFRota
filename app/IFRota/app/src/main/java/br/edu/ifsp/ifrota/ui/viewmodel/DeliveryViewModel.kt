package br.edu.ifsp.ifrota.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.edu.ifsp.ifrota.data.delivery.Delivery
import br.edu.ifsp.ifrota.data.delivery.DeliveryStatus
import br.edu.ifsp.ifrota.data.delivery.SampleDeliveries
import br.edu.ifsp.ifrota.data.local.entity.VehicleEntity
import br.edu.ifsp.ifrota.data.repository.VehicleRepository
import br.edu.ifsp.ifrota.data.simulation.DeliveryTrip
import br.edu.ifsp.ifrota.data.simulation.TelemetryPoint
import br.edu.ifsp.ifrota.data.simulation.VehicleStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Fase do trecho até a entrega atual. */
enum class DeliveryPhase {
    /** Antes de sair: rota do trecho vista de cima, veículo parado. */
    PLANNING,
    NAVIGATING,
    PAUSED,
    /** Chegou ao endereço; falta confirmar a entrega. */
    ARRIVED
}

data class DeliveryUiState(
    val vehicles: List<VehicleEntity> = emptyList(),
    /** Veículo do dia; com um só veículo cadastrado, ele é escolhido automaticamente. */
    val vehicle: VehicleEntity? = null,
    val deliveries: List<Delivery> = SampleDeliveries.today,
    val legs: List<DeliveryTrip> = DeliveryTrip.legs,
    /** Índice da entrega atual; igual a deliveries.size quando todas foram feitas. */
    val current: Int = 0,
    val phase: DeliveryPhase = DeliveryPhase.PLANNING,
    /** Posição atual em [trip]. */
    val index: Int = 0
) {
    val allDone: Boolean get() = current >= deliveries.size

    val currentDelivery: Delivery? get() = deliveries.getOrNull(current)

    /** Trecho até a entrega atual; depois da última, o fim do último trecho. */
    val trip: DeliveryTrip get() = legs[current.coerceAtMost(legs.lastIndex)]

    val position: TelemetryPoint get() = if (allDone) trip.points.last() else trip.points[index]

    val heading: Float get() = if (allDone) trip.headings.last() else trip.headings[index]

    /** O veículo é escolhido uma vez, antes de sair para a primeira entrega. */
    val vehicleLocked: Boolean get() = vehicle != null && (current > 0 || phase != DeliveryPhase.PLANNING)

    /** Telemetria exibida: fora da navegação o veículo está parado. */
    val telemetry: TelemetryPoint
        get() = when {
            allDone || phase == DeliveryPhase.PLANNING -> position.copy(speedKmh = 0, engineOn = false, doorsOpen = false)
            phase == DeliveryPhase.PAUSED -> position.copy(speedKmh = 0)
            else -> position
        }

    val status: VehicleStatus
        get() = if (telemetry.speedKmh > 0) VehicleStatus.IN_TRANSIT else VehicleStatus.STOPPED

    /** Trecho ainda a percorrer até a entrega atual. */
    val remainingPath: List<TelemetryPoint>
        get() = if (allDone) emptyList() else trip.points.subList(index, trip.points.size)

    val remainingMeters: Double get() = if (allDone) 0.0 else trip.remainingMeters(index)

    val remainingSeconds: Double get() = if (allDone) 0.0 else trip.remainingSeconds(index)

    /** Tempo estimado até concluir todas as entregas que faltam. */
    val remainingDaySeconds: Double
        get() = remainingSeconds + legs.drop(current + 1).sumOf { it.remainingSeconds(0) }

    val doneCount: Int get() = current.coerceAtMost(deliveries.size)

    val canStart: Boolean get() = !allDone && vehicle != null && phase == DeliveryPhase.PLANNING

    fun statusOf(delivery: Delivery): DeliveryStatus = when {
        delivery.order - 1 < current -> DeliveryStatus.DONE
        delivery.order - 1 == current && phase != DeliveryPhase.PLANNING -> DeliveryStatus.ACTIVE
        else -> DeliveryStatus.PENDING
    }

    /** Distância do trecho que leva até a entrega (da parada anterior ou do CD). */
    fun legMeters(delivery: Delivery): Double = legs[delivery.order - 1].remainingMeters(0)

    fun legSeconds(delivery: Delivery): Double = legs[delivery.order - 1].remainingSeconds(0)
}

private data class Progress(
    val current: Int = 0,
    val phase: DeliveryPhase = DeliveryPhase.PLANNING,
    val index: Int = 0
)

/**
 * Rota de entregas do dia: o motorista escolhe o veículo, faz as entregas na sequência e
 * cada trecho começa onde a entrega anterior foi feita. Fica no nível da tela principal
 * para que Início, Entregas e Rotas vejam o mesmo andamento.
 */
class DeliveryViewModel(
    repository: VehicleRepository,
    private val legs: List<DeliveryTrip> = DeliveryTrip.legs
) : ViewModel() {

    private val selectedVehicleId = MutableStateFlow<String?>(null)
    private val progress = MutableStateFlow(Progress())
    private var driving: Job? = null

    val state: StateFlow<DeliveryUiState> = combine(
        repository.getMyVehicles(),
        selectedVehicleId,
        progress
    ) { vehicles, selectedId, progress ->
        DeliveryUiState(
            vehicles = vehicles,
            vehicle = vehicles.find { it.id == selectedId } ?: vehicles.singleOrNull(),
            legs = legs,
            current = progress.current,
            phase = progress.phase,
            index = progress.index
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DeliveryUiState(legs = legs))

    init {
        // Se o veículo for excluído no meio de um trecho, o trecho é encerrado.
        viewModelScope.launch {
            state.collect { if (it.vehicle == null && it.phase != DeliveryPhase.PLANNING) end() }
        }
    }

    fun selectVehicle(id: String) {
        if (!state.value.vehicleLocked) selectedVehicleId.value = id
    }

    fun start() {
        val current = state.value
        if (!current.canStart) return
        // Fixa a escolha: cadastrar outro veículo depois não troca o veículo do dia.
        selectedVehicleId.value = current.vehicle?.id
        drive()
    }

    fun resume() {
        if (progress.value.phase == DeliveryPhase.PAUSED) drive()
    }

    fun pause() {
        if (progress.value.phase != DeliveryPhase.NAVIGATING) return
        driving?.cancel()
        progress.update { it.copy(phase = DeliveryPhase.PAUSED) }
    }

    /** Abandona o trecho atual: o veículo volta ao início dele, e a entrega continua pendente. */
    fun end() {
        driving?.cancel()
        progress.update { it.copy(phase = DeliveryPhase.PLANNING, index = 0) }
    }

    /** Confirma a entrega atual; o próximo trecho parte deste endereço. */
    fun confirmDelivery() {
        if (progress.value.phase != DeliveryPhase.ARRIVED) return
        progress.update { Progress(current = it.current + 1) }
    }

    /** Avança um ponto do trecho a cada tick até chegar ao endereço da entrega. */
    private fun drive() {
        driving?.cancel()
        progress.update { it.copy(phase = DeliveryPhase.NAVIGATING) }
        driving = viewModelScope.launch {
            val lastIndex = legs[progress.value.current].lastIndex
            while (progress.value.index < lastIndex) {
                delay(DeliveryTrip.TICK_MILLIS)
                progress.update { it.copy(index = it.index + 1) }
            }
            progress.update { it.copy(phase = DeliveryPhase.ARRIVED) }
        }
    }
}

class DeliveryViewModelFactory(
    private val repository: VehicleRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(DeliveryViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return DeliveryViewModel(repository) as T
    }
}
