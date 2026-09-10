package br.edu.ifsp.ifrota.poc_sqlite_firebase_sync.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.ifsp.ifrota.poc_sqlite_firebase_sync.data.local.entity.DriverEntity
import br.edu.ifsp.ifrota.poc_sqlite_firebase_sync.data.repository.DriverRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class DriverViewModel(private val repository: DriverRepository) : ViewModel() {

    val drivers: StateFlow<List<DriverEntity>> = repository.getAllDrivers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addDriver(name: String, cnh: String, phone: String) {
        viewModelScope.launch {
            repository.saveDriver(
                DriverEntity(id = UUID.randomUUID().toString(), name = name, cnh = cnh, phone = phone)
            )
        }
    }

    fun deleteDriver(driver: DriverEntity) {
        viewModelScope.launch { repository.deleteDriver(driver) }
    }
}
