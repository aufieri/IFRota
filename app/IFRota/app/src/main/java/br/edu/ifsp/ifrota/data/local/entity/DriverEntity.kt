package br.edu.ifsp.ifrota.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drivers")
data class DriverEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val cnh: String,
    val phone: String
)
