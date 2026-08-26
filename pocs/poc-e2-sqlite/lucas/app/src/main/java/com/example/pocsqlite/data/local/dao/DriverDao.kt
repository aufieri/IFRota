package com.example.pocsqlite.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.pocsqlite.data.local.entity.DriverEntity
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

    @Query("SELECT * FROM drivers ORDER BY name ASC")
    fun getAll(): Flow<List<DriverEntity>>
}
