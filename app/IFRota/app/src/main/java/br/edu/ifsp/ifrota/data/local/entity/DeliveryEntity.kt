package br.edu.ifsp.ifrota.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import br.edu.ifsp.ifrota.data.delivery.Delivery

/** Situação gravada da entrega; o andamento em curso (ativa) existe só na tela. */
object DeliveryRecordStatus {
    const val PENDING = "PENDING"
    const val DONE = "DONE"
}

/**
 * Entrega atribuída a um motorista pelo administrador. O [id] é o mesmo do documento no
 * Firestore (`<lote>_<n>`), o que torna a criação idempotente.
 */
@Entity(
    tableName = "deliveries",
    indices = [Index("driverId")]
)
data class DeliveryEntity(
    @PrimaryKey
    val id: String = "",

    /** UID do motorista responsável no Firebase Auth. */
    val driverId: String = "",

    /** Posição na fila do motorista, a partir de 1. */
    val orderNumber: Int = 0,

    val recipient: String = "",
    val address: String = "",
    val neighborhood: String = "",
    val city: String = "",
    val packageType: String = "",
    val note: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,

    /** [DeliveryRecordStatus.PENDING] ou [DeliveryRecordStatus.DONE]. */
    val status: String = DeliveryRecordStatus.PENDING,

    /**
     * Trecho viário da parada anterior (ou do CD, na primeira) até esta entrega, em
     * polyline codificada (ver `PolylineCodec`). Calculado pelo OSRM na criação.
     */
    val legPath: String = "",

    val createdAt: Long = System.currentTimeMillis(),

    val isSynced: Boolean = true,

    val updatedAt: Long = System.currentTimeMillis()
) {
    val isDone: Boolean get() = status == DeliveryRecordStatus.DONE

    fun toDomain(order: Int = orderNumber) = Delivery(
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
