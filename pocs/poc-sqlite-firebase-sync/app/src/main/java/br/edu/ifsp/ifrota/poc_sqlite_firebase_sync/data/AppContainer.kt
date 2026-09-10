package br.edu.ifsp.ifrota.poc_sqlite_firebase_sync.data

import android.content.Context
import br.edu.ifsp.ifrota.poc_sqlite_firebase_sync.data.local.AppDatabase
import br.edu.ifsp.ifrota.poc_sqlite_firebase_sync.data.repository.DriverRepository
import br.edu.ifsp.ifrota.poc_sqlite_firebase_sync.data.repository.VehicleRepository
import br.edu.ifsp.ifrota.poc_sqlite_firebase_sync.data.sync.SyncManager

object AppContainer {

    @Volatile
    private var syncManagerInstance: SyncManager? = null

    @Volatile
    private var driverRepositoryInstance: DriverRepository? = null

    @Volatile
    private var vehicleRepositoryInstance: VehicleRepository? = null

    fun syncManager(context: Context): SyncManager = syncManagerInstance ?: synchronized(this) {
        syncManagerInstance ?: run {
            val db = AppDatabase.getDatabase(context)
            SyncManager(db.driverDao(), db.vehicleDao()).also { syncManagerInstance = it }
        }
    }

    fun driverRepository(context: Context): DriverRepository = driverRepositoryInstance ?: synchronized(this) {
        driverRepositoryInstance ?: DriverRepository(
            AppDatabase.getDatabase(context).driverDao(),
            syncManager(context)
        ).also { driverRepositoryInstance = it }
    }

    fun vehicleRepository(context: Context): VehicleRepository = vehicleRepositoryInstance ?: synchronized(this) {
        vehicleRepositoryInstance ?: VehicleRepository(
            AppDatabase.getDatabase(context).vehicleDao(),
            syncManager(context)
        ).also { vehicleRepositoryInstance = it }
    }
}
