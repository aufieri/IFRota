package br.edu.ifsp.ifrota.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.edu.ifsp.ifrota.data.delivery.Delivery
import br.edu.ifsp.ifrota.data.delivery.DeliveryStatus
import br.edu.ifsp.ifrota.data.delivery.DistributionCenter
import br.edu.ifsp.ifrota.data.local.entity.DeliveryEntity
import br.edu.ifsp.ifrota.data.local.entity.VehicleEntity
import br.edu.ifsp.ifrota.data.repository.DeliveryRepository
import br.edu.ifsp.ifrota.data.repository.VehicleRepository
import br.edu.ifsp.ifrota.data.routing.PolylineCodec
import br.edu.ifsp.ifrota.data.simulation.DeliveryTrip
import br.edu.ifsp.ifrota.data.simulation.TelemetryPoint
import br.edu.ifsp.ifrota.data.simulation.VehicleStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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
    /** Entregas do motorista na ordem da fila (`order` = posição, a partir de 1). */
    val deliveries: List<Delivery> = emptyList(),
    /** Um trecho por entrega: o trecho i leva até a entrega i + 1. */
    val legs: List<DeliveryTrip> = emptyList(),
    /** Índice da entrega atual: a primeira ainda não concluída; igual a deliveries.size quando todas foram feitas. */
    val current: Int = 0,
    val phase: DeliveryPhase = DeliveryPhase.PLANNING,
    /** Posição atual em [trip]. */
    val index: Int = 0,
    /** O veículo é fixado ao iniciar o primeiro trecho da sessão. */
    val vehicleFixed: Boolean = false,
    /** Falso até o primeiro carregamento das entregas, para não mostrar "sem entregas" por engano. */
    val loaded: Boolean = false
) {
    val hasDeliveries: Boolean get() = deliveries.isNotEmpty()

    val allDone: Boolean get() = current >= deliveries.size

    val currentDelivery: Delivery? get() = deliveries.getOrNull(current)

    /** Trecho até a entrega atual; depois da última, o fim do último trecho; sem entregas, nenhum. */
    val trip: DeliveryTrip? get() = legs.getOrNull(current.coerceAtMost(legs.lastIndex))

    val position: TelemetryPoint
        get() {
            val trip = trip ?: return IDLE_POINT
            return if (allDone) trip.points.last() else trip.points[index.coerceAtMost(trip.lastIndex)]
        }

    val heading: Float
        get() {
            val trip = trip ?: return 0f
            return if (allDone) trip.headings.last() else trip.headings[index.coerceAtMost(trip.lastIndex)]
        }

    /** O veículo é escolhido uma vez, antes de sair para a primeira entrega. */
    val vehicleLocked: Boolean get() = vehicle != null && (vehicleFixed || phase != DeliveryPhase.PLANNING)

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
        get() = trip.takeUnless { allDone }
            ?.let { it.points.subList(index.coerceAtMost(it.lastIndex), it.points.size) }
            .orEmpty()

    val remainingMeters: Double
        get() = trip.takeUnless { allDone }?.let { it.remainingMeters(index.coerceAtMost(it.lastIndex)) } ?: 0.0

    val remainingSeconds: Double
        get() = trip.takeUnless { allDone }?.let { it.remainingSeconds(index.coerceAtMost(it.lastIndex)) } ?: 0.0

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
    fun legMeters(delivery: Delivery): Double = legs.getOrNull(delivery.order - 1)?.remainingMeters(0) ?: 0.0

    fun legSeconds(delivery: Delivery): Double = legs.getOrNull(delivery.order - 1)?.remainingSeconds(0) ?: 0.0

    private companion object {
        /** Posição de repouso quando o motorista ainda não tem entregas: o CD. */
        val IDLE_POINT = TelemetryPoint(
            DistributionCenter.LATITUDE, DistributionCenter.LONGITUDE,
            speedKmh = 0, engineOn = false, doorsOpen = false
        )
    }
}

private data class Progress(
    val phase: DeliveryPhase = DeliveryPhase.PLANNING,
    val index: Int = 0
)

private data class VehicleChoice(
    val selectedId: String? = null,
    /** Fixado ao iniciar o primeiro trecho da sessão. */
    val fixed: Boolean = false
)

/**
 * Rota do motorista: lê as entregas gravadas para ele (Room, sincronizado com o Firestore),
 * monta um trecho por entrega a partir do trajeto gravado em cada uma e conduz o motorista
 * pela fila. A entrega atual é a primeira ainda não concluída, então o andamento sobrevive
 * a reiniciar o app. Fica no nível da tela principal para que Início, Entregas e Rotas vejam
 * o mesmo andamento.
 *
 * A fila só cresce pelo fim (o administrador acrescenta entregas), então um trecho em curso
 * nunca muda por baixo do motorista.
 */
class DeliveryViewModel(
    vehicles: Flow<List<VehicleEntity>>,
    deliveries: Flow<List<DeliveryEntity>>,
    private val markDone: suspend (deliveryId: String) -> Unit
) : ViewModel() {

    private val vehicleChoice = MutableStateFlow(VehicleChoice())
    private val progress = MutableStateFlow(Progress())

    /** Entregas confirmadas nesta sessão, para a tela não voltar atrás enquanto o banco grava. */
    private val confirmedIds = MutableStateFlow<Set<String>>(emptySet())
    private var driving: Job? = null

    /** Evita refazer a reamostragem de um trecho a cada emissão da lista. */
    private val tripCache = HashMap<Pair<String, String>, DeliveryTrip>()

    val state: StateFlow<DeliveryUiState> = combine(
        vehicles,
        deliveries,
        vehicleChoice,
        progress,
        confirmedIds
    ) { vehicleList, entities, choice, progress, confirmed ->
        val queue = entities.sortedWith(compareBy({ it.orderNumber }, { it.createdAt }))
        val firstOpen = queue.indexOfFirst { !it.isDone && it.id !in confirmed }
        DeliveryUiState(
            vehicles = vehicleList,
            vehicle = vehicleList.find { it.id == choice.selectedId } ?: vehicleList.singleOrNull(),
            deliveries = queue.mapIndexed { i, entity -> entity.toDomain(order = i + 1) },
            legs = queue.map(::tripOf),
            current = if (firstOpen < 0) queue.size else firstOpen,
            phase = progress.phase,
            index = progress.index,
            vehicleFixed = choice.fixed,
            loaded = true
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DeliveryUiState())

    init {
        // Se o veículo for excluído no meio de um trecho, o trecho é encerrado.
        viewModelScope.launch {
            state.collect { if (it.vehicle == null && it.phase != DeliveryPhase.PLANNING) end() }
        }
    }

    private fun tripOf(entity: DeliveryEntity): DeliveryTrip =
        tripCache.getOrPut(entity.id to entity.legPath) {
            // Sem trajeto gravado (documento incompleto), o trecho degenera no próprio destino.
            DeliveryTrip.fromPath(
                PolylineCodec.decode(entity.legPath).ifEmpty { listOf(entity.latitude to entity.longitude) }
            )
        }

    fun selectVehicle(id: String) {
        if (!state.value.vehicleLocked) vehicleChoice.update { it.copy(selectedId = id) }
    }

    fun start() {
        val current = state.value
        if (!current.canStart) return
        // Fixa a escolha: cadastrar outro veículo depois não troca o veículo do dia.
        vehicleChoice.value = VehicleChoice(selectedId = current.vehicle?.id, fixed = true)
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
        progress.value = Progress()
    }

    /** Confirma a entrega atual e a grava como concluída; o próximo trecho parte deste endereço. */
    fun confirmDelivery() {
        if (progress.value.phase != DeliveryPhase.ARRIVED) return
        val delivery = state.value.currentDelivery ?: return
        confirmedIds.update { it + delivery.id }
        progress.value = Progress()
        viewModelScope.launch { markDone(delivery.id) }
    }

    /** Avança um ponto do trecho a cada tick até chegar ao endereço da entrega. */
    private fun drive() {
        val lastIndex = state.value.let { it.legs.getOrNull(it.current)?.lastIndex } ?: return
        driving?.cancel()
        progress.update { it.copy(phase = DeliveryPhase.NAVIGATING) }
        driving = viewModelScope.launch {
            while (progress.value.index < lastIndex) {
                delay(DeliveryTrip.TICK_MILLIS)
                progress.update { it.copy(index = it.index + 1) }
            }
            progress.update { it.copy(phase = DeliveryPhase.ARRIVED) }
        }
    }
}

class DeliveryViewModelFactory(
    private val vehicleRepository: VehicleRepository,
    private val deliveryRepository: DeliveryRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(DeliveryViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return DeliveryViewModel(
            vehicles = vehicleRepository.getMyVehicles(),
            deliveries = deliveryRepository.observeMyDeliveries(),
            markDone = { deliveryRepository.markDone(it) }
        ) as T
    }
}
