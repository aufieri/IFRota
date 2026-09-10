package br.edu.ifsp.ifrota.poc_sqlite_firebase_sync.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.ifsp.ifrota.poc_sqlite_firebase_sync.data.local.entity.VehicleEntity
import br.edu.ifsp.ifrota.poc_sqlite_firebase_sync.data.repository.VehicleRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class VehicleViewModel(private val repository: VehicleRepository) : ViewModel() {

    val vehicles: StateFlow<List<VehicleEntity>> = repository.getAllVehicles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addVehicle(plate: String, model: String, vehicleType: String, capacityKg: String) {
        viewModelScope.launch {
            repository.saveVehicle(
                VehicleEntity(
                    id = UUID.randomUUID().toString(),
                    plate = plate,
                    model = model,
                    vehicleType = vehicleType,
                    capacityKg = capacityKg.toDoubleOrNull() ?: 0.0
                )
            )
        }
    }

    fun deleteVehicle(vehicle: VehicleEntity) {
        viewModelScope.launch { repository.deleteVehicle(vehicle) }
    }
}
