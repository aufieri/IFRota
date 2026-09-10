package br.edu.ifsp.ifrota.poc_sqlite_firebase_sync.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.ifsp.ifrota.poc_sqlite_firebase_sync.data.sync.SyncManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SyncViewModel(private val syncManager: SyncManager) : ViewModel() {

    var isSyncing by mutableStateOf(false)
        private set

    var statusMessage by mutableStateOf<String?>(null)
        private set

    init {
        syncManager.startListening()
    }

    fun syncNow() {
        if (isSyncing) return
        viewModelScope.launch {
            isSyncing = true
            statusMessage = null
            try {
                syncManager.syncAll()
                val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                statusMessage = "Sincronizado às $time"
            } catch (e: Exception) {
                statusMessage = "Falha ao sincronizar: ${e.message}"
            } finally {
                isSyncing = false
            }
        }
    }

    override fun onCleared() {
        syncManager.stopListening()
    }
}
