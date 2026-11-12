package br.edu.ifsp.ifrota.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import br.edu.ifsp.ifrota.data.delivery.Delivery

@Entity(
    tableName = "deliveries",
    indices = [Index("driverId")]
)
data class DeliveryEntity(
    @PrimaryKey
    val id: String = "",
    val driverId: String = "",
    val orderNumber: Int = 0,
    val recipient: String = "",
    val address: String = "",
    val neighborhood: String = "",
    val city: String = "",
    val packageType: String = "",
    val note: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val status: String = "PENDING",
    val legPath: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isDone: Boolean get() = status == "DONE"

    fun toDomain(order: Int): Delivery = Delivery(
        id = id,
        order = order,
        recipient = recipient,
        address = address,
        neighborhood = neighborhood,
        city = city,
        packageType = packageType,
        note = note,
        latitude = latitude,
        longitude = longitude
    )
}
