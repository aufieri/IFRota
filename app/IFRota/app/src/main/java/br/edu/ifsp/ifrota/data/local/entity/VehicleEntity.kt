package br.edu.ifsp.ifrota.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vehicles",
    indices = [Index("ownerId")]
)
data class VehicleEntity(
    @PrimaryKey
    val id: String = "",

    /** UID do usuário dono do veículo no Firebase Auth. */
    val ownerId: String = "",

    val plate: String = "",
    val model: String = "",
    val vehicleType: String = "",
    val capacityKg: Double = 0.0,

    val isSynced: Boolean = false,

    val isDeleted: Boolean = false,

    val updatedAt: Long = System.currentTimeMillis()
)
