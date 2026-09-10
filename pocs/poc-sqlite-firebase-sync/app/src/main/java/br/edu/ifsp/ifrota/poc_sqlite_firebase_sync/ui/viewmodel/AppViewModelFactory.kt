package br.edu.ifsp.ifrota.poc_sqlite_firebase_sync.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import br.edu.ifsp.ifrota.poc_sqlite_firebase_sync.data.AppContainer

class AppViewModelFactory(context: Context) : ViewModelProvider.Factory {

    private val appContext = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(DriverViewModel::class.java) ->
                DriverViewModel(AppContainer.driverRepository(appContext)) as T

            modelClass.isAssignableFrom(VehicleViewModel::class.java) ->
                VehicleViewModel(AppContainer.vehicleRepository(appContext)) as T

            modelClass.isAssignableFrom(SyncViewModel::class.java) ->
                SyncViewModel(AppContainer.syncManager(appContext)) as T

            else -> throw IllegalArgumentException("ViewModel desconhecido: ${modelClass.name}")
        }
    }
}
