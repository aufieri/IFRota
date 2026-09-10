package br.edu.ifsp.ifrota.poc_sqlite_firebase_sync.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.edu.ifsp.ifrota.poc_sqlite_firebase_sync.data.local.entity.VehicleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vehicle: VehicleEntity)

    @Update
    suspend fun update(vehicle: VehicleEntity)

    @Delete
    suspend fun delete(vehicle: VehicleEntity)

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun getById(id: String): VehicleEntity?


    @Query("SELECT * FROM vehicles WHERE isDeleted = 0 ORDER BY plate ASC")
    fun getAll(): Flow<List<VehicleEntity>>


    @Query("SELECT * FROM vehicles WHERE isSynced = 0")
    suspend fun getUnsynced(): List<VehicleEntity>


    @Query("UPDATE vehicles SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)


    @Query("DELETE FROM vehicles WHERE id = :id AND isDeleted = 1")
    suspend fun purgeIfDeleted(id: String)


    @androidx.room.Upsert
    suspend fun upsert(vehicle: VehicleEntity)
}
