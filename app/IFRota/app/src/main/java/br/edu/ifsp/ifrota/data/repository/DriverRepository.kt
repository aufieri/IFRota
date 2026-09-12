package br.edu.ifsp.ifrota.data.repository

import br.edu.ifsp.ifrota.data.local.dao.DriverDao
import br.edu.ifsp.ifrota.data.local.entity.DriverEntity
import br.edu.ifsp.ifrota.data.sync.SyncManager
import kotlinx.coroutines.flow.Flow
class DriverRepository(
    private val userId: String,
    private val driverDao: DriverDao,
    private val syncManager: SyncManager
) {

    fun observeMyProfile(): Flow<DriverEntity?> = driverDao.observeById(userId)

    suspend fun getMyProfile(): DriverEntity? = driverDao.getById(userId)

    suspend fun saveMyProfile(
        name: String,
        email: String,
        phone: String,
        cnh: String
    ) {
        val profile = DriverEntity(
            id = userId,
            name = name.trim(),
            email = email.trim(),
            phone = phone.trim(),
            cnh = cnh.trim(),
            isSynced = false,
            isDeleted = false,
            updatedAt = System.currentTimeMillis()
        )
        driverDao.upsert(profile)
        syncManager.pushDrivers()
    }

    suspend fun deleteMyProfile() {
        val profile = driverDao.getById(userId) ?: return
        val deletedProfile = profile.copy(
            isDeleted = true,
            isSynced = false,
            updatedAt = System.currentTimeMillis()
        )
        driverDao.upsert(deletedProfile)
        syncManager.pushDrivers()
    }
}