package br.edu.ifsp.ifrota.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.edu.ifsp.ifrota.data.local.entity.DriverEntity
import br.edu.ifsp.ifrota.data.repository.DriverRepository
import br.edu.ifsp.ifrota.data.repository.VehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileFormState(
    val name: String = "",
    val phone: String = "",
    val cnh: String = "",
    val nameError: String? = null,
    val isSaving: Boolean = false
)

class ProfileViewModel(
    private val driverRepository: DriverRepository,
    vehicleRepository: VehicleRepository,
    private val accountEmail: String
) : ViewModel() {

    val profile: StateFlow<DriverEntity?> = driverRepository.observeMyProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val vehicleCount: StateFlow<Int> = vehicleRepository.countMyVehicles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _form = MutableStateFlow(ProfileFormState())
    val form: StateFlow<ProfileFormState> = _form.asStateFlow()

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing.asStateFlow()

    fun onNameChange(value: String) = _form.update { it.copy(name = value, nameError = null) }

    fun onPhoneChange(value: String) = _form.update { it.copy(phone = value) }

    fun onCnhChange(value: String) = _form.update { it.copy(cnh = value) }

    fun startEditing() {
        val current = profile.value
        _form.value = ProfileFormState(
            name = current?.name.orEmpty(),
            phone = current?.phone.orEmpty(),
            cnh = current?.cnh.orEmpty()
        )
        _isEditing.value = true
    }

    fun cancelEditing() {
        _isEditing.value = false
        _form.value = ProfileFormState()
    }

    fun saveProfile() {
        val state = _form.value
        if (state.name.isBlank()) {
            _form.update { it.copy(nameError = "Informe seu nome") }
            return
        }

        _form.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            driverRepository.saveMyProfile(
                name = state.name,
                email = profile.value?.email?.takeIf { it.isNotBlank() } ?: accountEmail,
                phone = state.phone,
                cnh = state.cnh
            )
            cancelEditing()
        }
    }
}

class ProfileViewModelFactory(
    private val driverRepository: DriverRepository,
    private val vehicleRepository: VehicleRepository,
    private val accountEmail: String
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return ProfileViewModel(driverRepository, vehicleRepository, accountEmail) as T
    }
}
