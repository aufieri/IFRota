package com.example.pocgeofencing.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocgeofencing.data.FleetSimulator
import com.example.pocgeofencing.data.Geofence
import com.example.pocgeofencing.data.GeofenceEvent
import com.example.pocgeofencing.data.GeofenceMonitor
import com.example.pocgeofencing.data.SampleGeofences
import com.example.pocgeofencing.data.VehiclePosition
import com.example.pocgeofencing.data.VehicleSnapshot
import com.example.pocgeofencing.notification.GeofenceNotifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GeofenceUiState(
    val fleet: List<VehicleSnapshot> = emptyList(),
    val geofences: List<Geofence> = emptyList(),
    /** Perímetros com algum veículo dentro agora. */
    val occupied: Set<String> = emptySet(),
    /** Eventos mais recentes primeiro. */
    val events: List<GeofenceEvent> = emptyList()
)

/**
 * Liga o simulador da frota ao [GeofenceMonitor]: a cada tick, as novas posições passam pelo
 * monitor e cada entrada ou saída vira uma notificação e uma linha no histórico.
 *
 * A coleta roda no viewModelScope (e não só enquanto a tela está visível), então as
 * notificações continuam chegando com o app em segundo plano.
 */
class GeofenceViewModel(application: Application) : AndroidViewModel(application) {

    private val notifier = GeofenceNotifier(application)
    private val monitor = GeofenceMonitor(SampleGeofences.all)

    private val _state = MutableStateFlow(GeofenceUiState(geofences = monitor.geofences))
    val state: StateFlow<GeofenceUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            FleetSimulator().snapshots().collect { fleet ->
                val events = monitor.update(
                    fleet.map {
                        VehiclePosition(it.vehicle.id, it.vehicle.plate, it.telemetry.latitude, it.telemetry.longitude)
                    }
                )
                events.forEach(notifier::notify)
                _state.update { old ->
                    old.copy(
                        fleet = fleet,
                        occupied = fleet.flatMapTo(HashSet()) { monitor.insideOf(it.vehicle.id) },
                        events = (events.reversed() + old.events).take(MAX_EVENTS)
                    )
                }
            }
        }
    }

    fun canNotify(): Boolean = notifier.canNotify()

    private companion object {
        const val MAX_EVENTS = 50
    }
}
