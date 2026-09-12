package br.edu.ifsp.ifrota.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.edu.ifsp.ifrota.data.local.entity.DriverEntity
import br.edu.ifsp.ifrota.data.local.entity.VehicleEntity
import br.edu.ifsp.ifrota.data.repository.DriverRepository
import br.edu.ifsp.ifrota.data.repository.VehicleRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(
    driverRepository: DriverRepository,
    vehicleRepository: VehicleRepository
) : ViewModel() {

    val profile: StateFlow<DriverEntity?> = driverRepository.observeMyProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val vehicles: StateFlow<List<VehicleEntity>> = vehicleRepository.getMyVehicles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

class DashboardViewModelFactory(
    private val driverRepository: DriverRepository,
    private val vehicleRepository: VehicleRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return DashboardViewModel(driverRepository, vehicleRepository) as T
    }
}
