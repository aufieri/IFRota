package com.example.pocsqlite.data.repository

import com.example.pocsqlite.data.local.dao.DriverDao
import com.example.pocsqlite.data.local.entity.DriverEntity
import kotlinx.coroutines.flow.Flow

class DriverRepository(private val driverDao: DriverDao) {

    fun getAllDrivers(): Flow<List<DriverEntity>> = driverDao.getAll()

    suspend fun getDriverById(id: String): DriverEntity? = driverDao.getById(id)

    suspend fun saveDriver(driver: DriverEntity) = driverDao.insert(driver)

    suspend fun updateDriver(driver: DriverEntity) = driverDao.update(driver)

    suspend fun deleteDriver(driver: DriverEntity) = driverDao.delete(driver)
}
