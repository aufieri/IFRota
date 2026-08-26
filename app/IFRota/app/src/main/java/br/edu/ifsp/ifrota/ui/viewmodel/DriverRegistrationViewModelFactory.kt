package br.edu.ifsp.ifrota.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import br.edu.ifsp.ifrota.data.repository.DriverRepository

class DriverRegistrationViewModelFactory(
    private val repository: DriverRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(DriverRegistrationViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return DriverRegistrationViewModel(repository) as T
    }
}
