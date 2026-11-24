package br.edu.ifsp.ifrota.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.edu.ifsp.ifrota.data.geofencing.Geofence
import br.edu.ifsp.ifrota.data.geofencing.GeofenceEvent
import br.edu.ifsp.ifrota.data.geofencing.GeofenceTransition
import br.edu.ifsp.ifrota.data.tracking.AccountInfo
import br.edu.ifsp.ifrota.data.tracking.FenceEventRecord
import br.edu.ifsp.ifrota.data.tracking.LiveLocation
import br.edu.ifsp.ifrota.data.tracking.TrackingRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TrackedDriver(
    val location: LiveLocation,
    val name: String,
    val online: Boolean
) {
    val id: String get() = location.driverId
}

data class AdminFenceEvent(val record: FenceEventRecord, val driverName: String) {
    val message: String
        get() = "$driverName (${record.plate}) ${
            if (record.transition == GeofenceTransition.ENTER) "entrou no perímetro" else "saiu do perímetro"
        }: ${record.fenceName}"
}

data class AdminUiState(
    val drivers: List<TrackedDriver> = emptyList(),
    val events: List<AdminFenceEvent> = emptyList(),
    val now: Long = System.currentTimeMillis(),
    val loaded: Boolean = false,
    val error: String? = null
) {
    val onlineCount: Int get() = drivers.count { it.online }
}

/**
 * Painel do administrador: acompanha em tempo real a posição publicada por cada entregador
 * e os eventos de perímetro, avisando cada evento novo por [onNewEvent] (notificação).
 *
 * Os listeners do Firestore ficam ativos enquanto alguém observa [state] (a tela do admin,
 * inclusive com o app em segundo plano) e são desligados 5 s depois de sair da conta.
 */
class AdminTrackingViewModel(
    repository: TrackingRepository,
    private val adminId: String,
    private val onNewEvent: (GeofenceEvent) -> Unit
) : ViewModel() {

    private val error = MutableStateFlow<String?>(null)

    /** Eventos já vistos; o primeiro lote (histórico) é só registrado, sem notificar. */
    private var seenEvents: MutableSet<String>? = null

    init {
        // Se esta conta já foi usada como entregador, a posição antiga sai da frota.
        viewModelScope.launch { repository.deleteOwnLocation(adminId) }
    }

    private val accounts: Flow<Map<String, AccountInfo>> = repository.observeAccounts()
        .catch { reportError(it); emit(emptyMap()) }

    private val locations: Flow<List<LiveLocation>?> = repository.observeLocations()
        .catch { reportError(it); emit(emptyList()) }

    private val events: Flow<List<FenceEventRecord>> = repository.observeRecentEvents()
        .catch { reportError(it); emit(emptyList()) }

    /** Relógio para marcar como offline quem parou de mandar posição. */
    private val clock: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(5_000)
        }
    }

    private var latestAccounts: Map<String, AccountInfo> = emptyMap()

    /** Administrador não é entregador: nem ele nem outra conta admin entram na frota. */
    private fun isDriver(id: String, accounts: Map<String, AccountInfo>): Boolean =
        id != adminId && accounts[id]?.isAdmin != true

    val state: StateFlow<AdminUiState> = combine(
        accounts.onEach { latestAccounts = it },
        locations,
        events.onEach(::notifyNew),
        clock,
        error
    ) { accounts, locations, events, now, error ->
        AdminUiState(
            drivers = locations.orEmpty()
                .filter { isDriver(it.driverId, accounts) }
                .map { TrackedDriver(it, accounts[it.driverId]?.name ?: it.plate.ifBlank { "Entregador" }, it.isOnline(now)) }
                .sortedWith(compareByDescending<TrackedDriver> { it.online }.thenBy { it.name }),
            events = events
                .filter { isDriver(it.driverId, accounts) }
                .map { AdminFenceEvent(it, accounts[it.driverId]?.name ?: "Entregador") },
            now = now,
            loaded = locations != null,
            error = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdminUiState())

    private fun notifyNew(records: List<FenceEventRecord>) {
        val seen = seenEvents
        if (seen == null) {
            seenEvents = records.mapTo(HashSet()) { it.id }
            return
        }
        // Do mais antigo para o mais novo, para as notificações saírem na ordem dos fatos.
        records.filter { seen.add(it.id) && isDriver(it.driverId, latestAccounts) }.reversed().forEach { record ->
            val name = latestAccounts[record.driverId]?.name ?: "Entregador"
            onNewEvent(
                GeofenceEvent(
                    vehicleId = record.driverId,
                    plate = "$name (${record.plate})",
                    geofence = Geofence(record.fenceId, record.fenceName, 0.0, 0.0, 0.0),
                    transition = record.transition,
                    timestampMillis = record.timestampMillis
                )
            )
        }
    }

    private fun reportError(e: Throwable) {
        Log.w("AdminTracking", "Falha ao acompanhar a frota", e)
        error.value = if (e is FirebaseFirestoreException &&
            e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        ) {
            "Sem permissão para ler a frota. Publique as regras atualizadas do Firestore (firestore.rules)."
        } else {
            "Não foi possível acompanhar a frota: ${e.message}"
        }
    }
}

class AdminTrackingViewModelFactory(
    private val repository: TrackingRepository,
    private val adminId: String,
    private val onNewEvent: (GeofenceEvent) -> Unit
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AdminTrackingViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return AdminTrackingViewModel(repository, adminId, onNewEvent) as T
    }
}
