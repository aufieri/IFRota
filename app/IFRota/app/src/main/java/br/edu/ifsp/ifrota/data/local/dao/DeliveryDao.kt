package br.edu.ifsp.ifrota.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import br.edu.ifsp.ifrota.data.local.entity.DeliveryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeliveryDao {

    @Upsert
    suspend fun upsert(delivery: DeliveryEntity)

    @Query("SELECT * FROM deliveries WHERE id = :id")
    suspend fun getById(id: String): DeliveryEntity?

    @Query("SELECT * FROM deliveries WHERE driverId = :driverId ORDER BY orderNumber ASC, createdAt ASC")
    fun observeByDriver(driverId: String): Flow<List<DeliveryEntity>>

    @Query("SELECT * FROM deliveries WHERE driverId = :driverId ORDER BY orderNumber ASC, createdAt ASC")
    suspend fun getByDriver(driverId: String): List<DeliveryEntity>

    @Query("SELECT * FROM deliveries WHERE isSynced = 0")
    suspend fun getUnsynced(): List<DeliveryEntity>

    @Query("UPDATE deliveries SET isSynced = 1 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markAsSynced(id: String, updatedAt: Long)

    /** Só o motorista dono muda o status da própria entrega, e só para concluída. */
    @Query(
        "UPDATE deliveries SET status = 'DONE', isSynced = 0, updatedAt = :updatedAt " +
            "WHERE id = :id AND driverId = :driverId AND status <> 'DONE'"
    )
    suspend fun markDone(id: String, driverId: String, updatedAt: Long): Int

    @Query("DELETE FROM deliveries WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM deliveries")
    suspend fun clearAll()
}
