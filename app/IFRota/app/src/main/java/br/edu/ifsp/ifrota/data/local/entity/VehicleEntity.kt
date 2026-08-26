package br.edu.ifsp.ifrota.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey
    val id: String,
    val plate: String,
    val model: String,
    val vehicleType: String,
    val capacityKg: Double
)
