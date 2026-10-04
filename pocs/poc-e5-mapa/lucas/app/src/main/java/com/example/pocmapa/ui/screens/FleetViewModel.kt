package com.example.pocmapa.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocmapa.data.FleetSimulator
import com.example.pocmapa.data.VehicleSnapshot
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class FleetViewModel(simulator: FleetSimulator = FleetSimulator()) : ViewModel() {

    val fleet: StateFlow<List<VehicleSnapshot>> = simulator.snapshots()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
