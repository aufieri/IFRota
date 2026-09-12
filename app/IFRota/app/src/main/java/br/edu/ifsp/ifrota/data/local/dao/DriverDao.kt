package br.edu.ifsp.ifrota.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import br.edu.ifsp.ifrota.data.local.entity.DriverEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DriverDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(driver: DriverEntity)

    @Update
    suspend fun update(driver: DriverEntity)

    @Delete
    suspend fun delete(driver: DriverEntity)

    @Query("SELECT * FROM drivers WHERE id = :id")
    suspend fun getById(id: String): DriverEntity?

    @Query("SELECT * FROM drivers WHERE id = :id AND isDeleted = 0")
    fun observeById(id: String): Flow<DriverEntity?>

    @Query("SELECT * FROM drivers WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAll(): Flow<List<DriverEntity>>

    @Query("SELECT * FROM drivers WHERE isSynced = 0")
    suspend fun getUnsynced(): List<DriverEntity>

    @Query("UPDATE drivers SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Query("DELETE FROM drivers WHERE id = :id AND isDeleted = 1")
    suspend fun purgeIfDeleted(id: String)

    @Query("DELETE FROM drivers")
    suspend fun clearAll()

    @Upsert
    suspend fun upsert(driver: DriverEntity)
}
