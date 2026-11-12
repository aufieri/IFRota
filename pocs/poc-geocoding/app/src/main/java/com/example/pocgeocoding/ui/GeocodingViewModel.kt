package com.example.pocgeocoding.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocgeocoding.data.GeocodingRepository
import com.example.pocgeocoding.data.ReverseGeocodingResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GeocodingUiState(
    val lat: String = "-21.7843",
    val lon: String = "-48.2105",
    val loading: Boolean = false,
    val result: ReverseGeocodingResult? = null,
    val error: String? = null
)

class GeocodingViewModel : ViewModel() {

    private val _state = MutableStateFlow(GeocodingUiState())
    val state = _state.asStateFlow()

    fun onLatChange(value: String) = _state.update { it.copy(lat = value, error = null) }
    fun onLonChange(value: String) = _state.update { it.copy(lon = value, error = null) }

    fun search() {
        val lat = _state.value.lat.toDoubleOrNull()
        val lon = _state.value.lon.toDoubleOrNull()
        if (lat == null || lon == null) {
            _state.update { it.copy(error = "Latitude e longitude devem ser números válidos.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, result = null, error = null) }
            GeocodingRepository.reverseGeocode(lat, lon)
                .onSuccess { r -> _state.update { it.copy(loading = false, result = r) } }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.message ?: "Erro desconhecido") } }
        }
    }
}
