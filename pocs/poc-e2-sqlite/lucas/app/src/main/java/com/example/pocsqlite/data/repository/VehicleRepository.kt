package com.example.pocsqlite.data.repository

import com.example.pocsqlite.data.local.dao.VehicleDao
import com.example.pocsqlite.data.local.entity.VehicleEntity
import kotlinx.coroutines.flow.Flow

class VehicleRepository(private val vehicleDao: VehicleDao) {

    fun getAllVehicles(): Flow<List<VehicleEntity>> = vehicleDao.getAll()

    suspend fun getVehicleById(id: String): VehicleEntity? = vehicleDao.getById(id)

    suspend fun saveVehicle(vehicle: VehicleEntity) = vehicleDao.insert(vehicle)

    suspend fun updateVehicle(vehicle: VehicleEntity) = vehicleDao.update(vehicle)

    suspend fun deleteVehicle(vehicle: VehicleEntity) = vehicleDao.delete(vehicle)
}
