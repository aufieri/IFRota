package br.edu.ifsp.ifrota.data.repository

import br.edu.ifsp.ifrota.data.local.dao.DriverDao
import br.edu.ifsp.ifrota.data.local.entity.DriverEntity
import br.edu.ifsp.ifrota.data.sync.SyncManager
import kotlinx.coroutines.flow.Flow

class DriverRepository(
    private val driverDao: DriverDao,
    private val syncManager: SyncManager
) {

    fun getAllDrivers(): Flow<List<DriverEntity>> = driverDao.getAll()

    suspend fun getDriverById(id: String): DriverEntity? = driverDao.getById(id)

    suspend fun saveDriver(driver: DriverEntity) {
        val pendente = driver.copy(isSynced = false, updatedAt = System.currentTimeMillis())
        driverDao.insert(pendente)
        syncManager.pushDrivers()
    }

    suspend fun updateDriver(driver: DriverEntity) {
        val pendente = driver.copy(isSynced = false, updatedAt = System.currentTimeMillis())
        driverDao.update(pendente)
        syncManager.pushDrivers()
    }

    suspend fun deleteDriver(driver: DriverEntity) {
        val marcado = driver.copy(isDeleted = true, isSynced = false, updatedAt = System.currentTimeMillis())
        driverDao.update(marcado)
        syncManager.pushDrivers()
    }
}