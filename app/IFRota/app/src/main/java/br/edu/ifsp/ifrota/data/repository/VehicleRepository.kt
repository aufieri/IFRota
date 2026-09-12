package br.edu.ifsp.ifrota.data.repository

import br.edu.ifsp.ifrota.data.local.dao.VehicleDao
import br.edu.ifsp.ifrota.data.local.entity.VehicleEntity
import br.edu.ifsp.ifrota.data.sync.SyncManager
import kotlinx.coroutines.flow.Flow

class VehicleRepository(
    private val userId: String,
    private val vehicleDao: VehicleDao,
    private val syncManager: SyncManager
) {

    fun getMyVehicles(): Flow<List<VehicleEntity>> = vehicleDao.getAllByOwner(userId)

    fun countMyVehicles(): Flow<Int> = vehicleDao.countByOwner(userId)

    suspend fun getVehicleById(id: String): VehicleEntity? = vehicleDao.getById(id)

    suspend fun saveVehicle(vehicle: VehicleEntity) {
        val pendente = vehicle.copy(
            ownerId = userId,
            isSynced = false,
            updatedAt = System.currentTimeMillis()
        )
        vehicleDao.insert(pendente)
        syncManager.pushVehicles()
    }

    suspend fun updateVehicle(vehicle: VehicleEntity) {
        val pendente = vehicle.copy(
            ownerId = userId,
            isSynced = false,
            updatedAt = System.currentTimeMillis()
        )
        vehicleDao.update(pendente)
        syncManager.pushVehicles()
    }

    suspend fun deleteVehicle(vehicle: VehicleEntity) {
        val marcado = vehicle.copy(
            isDeleted = true,
            isSynced = false,
            updatedAt = System.currentTimeMillis()
        )
        vehicleDao.update(marcado)
        syncManager.pushVehicles()
    }
}
