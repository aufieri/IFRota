package br.edu.ifsp.ifrota.poc_sqlite_firebase_sync.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey
    val id: String = "",
    val plate: String = "",
    val model: String = "",
    val vehicleType: String = "",
    val capacityKg: Double = 0.0,

    val isSynced: Boolean = false,

    val isDeleted: Boolean = false,

    val updatedAt: Long = System.currentTimeMillis()
)
