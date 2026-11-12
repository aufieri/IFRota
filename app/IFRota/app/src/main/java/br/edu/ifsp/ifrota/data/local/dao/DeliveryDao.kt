package br.edu.ifsp.ifrota.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import br.edu.ifsp.ifrota.data.local.entity.DeliveryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeliveryDao {

    @Query("SELECT * FROM deliveries WHERE driverId = :driverId ORDER BY orderNumber ASC, createdAt ASC")
    fun observeForDriver(driverId: String): Flow<List<DeliveryEntity>>

    @Query("SELECT * FROM deliveries WHERE isSynced = 0")
    suspend fun getUnsynced(): List<DeliveryEntity>

    @Query("SELECT * FROM deliveries WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DeliveryEntity?

    @Upsert
    suspend fun upsert(entity: DeliveryEntity)

    @Query("UPDATE deliveries SET isSynced = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markAsSynced(id: String, updatedAt: Long)

    @Query("UPDATE deliveries SET status = 'DONE', isSynced = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markDone(id: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM deliveries WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM deliveries")
    suspend fun clearAll()
}
