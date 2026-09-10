package br.edu.ifsp.ifrota.poc_sqlite_firebase_sync.data.repository

import br.edu.ifsp.ifrota.poc_sqlite_firebase_sync.data.local.dao.VehicleDao
import br.edu.ifsp.ifrota.poc_sqlite_firebase_sync.data.local.entity.VehicleEntity
import br.edu.ifsp.ifrota.poc_sqlite_firebase_sync.data.sync.SyncManager
import kotlinx.coroutines.flow.Flow

class VehicleRepository(
    private val vehicleDao: VehicleDao,
    private val syncManager: SyncManager
) {

    fun getAllVehicles(): Flow<List<VehicleEntity>> = vehicleDao.getAll()

    suspend fun getVehicleById(id: String): VehicleEntity? = vehicleDao.getById(id)

    suspend fun saveVehicle(vehicle: VehicleEntity) {
        val pendente = vehicle.copy(isSynced = false, updatedAt = System.currentTimeMillis())
        vehicleDao.insert(pendente)
        syncManager.pushVehicles()
    }

    suspend fun updateVehicle(vehicle: VehicleEntity) {
        val pendente = vehicle.copy(isSynced = false, updatedAt = System.currentTimeMillis())
        vehicleDao.update(pendente)
        syncManager.pushVehicles()
    }

    suspend fun deleteVehicle(vehicle: VehicleEntity) {
        val marcado = vehicle.copy(isDeleted = true, isSynced = false, updatedAt = System.currentTimeMillis())
        vehicleDao.update(marcado)
        syncManager.pushVehicles()
    }
}