package br.edu.ifsp.ifrota.data.delivery

import br.edu.ifsp.ifrota.data.local.entity.DeliveryEntity
import br.edu.ifsp.ifrota.data.local.entity.DeliveryRecordStatus
import com.google.firebase.firestore.DocumentSnapshot

/**
 * Conversão entre [DeliveryEntity] e o documento da coleção `deliveries`. Os campos são
 * lidos e escritos um a um (em vez de por reflexão) para que o formato no Firestore fique
 * explícito e [DeliveryEntity.isSynced], que só existe localmente, nunca seja enviado.
 */
object DeliveryDocument {
    const val COLLECTION = "deliveries"

    const val DRIVER_ID = "driverId"
    const val STATUS = "status"
    const val UPDATED_AT = "updatedAt"

    fun toMap(delivery: DeliveryEntity): Map<String, Any?> = mapOf(
        DRIVER_ID to delivery.driverId,
        "orderNumber" to delivery.orderNumber,
        "recipient" to delivery.recipient,
        "address" to delivery.address,
        "neighborhood" to delivery.neighborhood,
        "city" to delivery.city,
        "packageType" to delivery.packageType,
        "note" to delivery.note,
        "latitude" to delivery.latitude,
        "longitude" to delivery.longitude,
        STATUS to delivery.status,
        "legPath" to delivery.legPath,
        "createdAt" to delivery.createdAt,
        UPDATED_AT to delivery.updatedAt
    )

    /** Devolve null se o documento não tiver o mínimo para ser uma entrega válida. */
    fun fromSnapshot(snapshot: DocumentSnapshot): DeliveryEntity? {
        val driverId = snapshot.getString(DRIVER_ID) ?: return null
        val latitude = snapshot.getDouble("latitude") ?: return null
        val longitude = snapshot.getDouble("longitude") ?: return null
        val status = snapshot.getString(STATUS)
            ?.takeIf { it == DeliveryRecordStatus.DONE }
            ?: DeliveryRecordStatus.PENDING
        return DeliveryEntity(
            id = snapshot.id,
            driverId = driverId,
            orderNumber = snapshot.getLong("orderNumber")?.toInt() ?: 0,
            recipient = snapshot.getString("recipient").orEmpty(),
            address = snapshot.getString("address").orEmpty(),
            neighborhood = snapshot.getString("neighborhood").orEmpty(),
            city = snapshot.getString("city").orEmpty(),
            packageType = snapshot.getString("packageType").orEmpty(),
            note = snapshot.getString("note"),
            latitude = latitude,
            longitude = longitude,
            status = status,
            legPath = snapshot.getString("legPath").orEmpty(),
            createdAt = snapshot.getLong("createdAt") ?: 0L,
            isSynced = true,
            updatedAt = snapshot.getLong(UPDATED_AT) ?: 0L
        )
    }
}
