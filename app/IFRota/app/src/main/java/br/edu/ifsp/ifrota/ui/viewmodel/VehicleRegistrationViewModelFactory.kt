package br.edu.ifsp.ifrota.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import br.edu.ifsp.ifrota.data.repository.VehicleRepository

class VehicleRegistrationViewModelFactory(
    private val repository: VehicleRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(VehicleRegistrationViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return VehicleRegistrationViewModel(repository) as T
    }
}
