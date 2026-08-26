package br.edu.ifsp.ifrota.data.repository

import br.edu.ifsp.ifrota.data.local.dao.VehicleDao
import br.edu.ifsp.ifrota.data.local.entity.VehicleEntity
import kotlinx.coroutines.flow.Flow

class VehicleRepository(private val vehicleDao: VehicleDao) {

    fun getAllVehicles(): Flow<List<VehicleEntity>> = vehicleDao.getAll()

    suspend fun getVehicleById(id: String): VehicleEntity? = vehicleDao.getById(id)

    suspend fun saveVehicle(vehicle: VehicleEntity) = vehicleDao.insert(vehicle)

    suspend fun updateVehicle(vehicle: VehicleEntity) = vehicleDao.update(vehicle)

    suspend fun deleteVehicle(vehicle: VehicleEntity) = vehicleDao.delete(vehicle)
}
