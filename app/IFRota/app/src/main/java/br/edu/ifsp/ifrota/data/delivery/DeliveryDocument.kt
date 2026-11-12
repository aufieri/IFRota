package br.edu.ifsp.ifrota.data.delivery

import br.edu.ifsp.ifrota.data.local.entity.DeliveryEntity
import com.google.firebase.firestore.DocumentSnapshot

object DeliveryDocument {
    const val COLLECTION = "deliveries"
    const val DRIVER_ID = "driverId"
    const val STATUS = "status"
    const val UPDATED_AT = "updatedAt"

    fun fromSnapshot(doc: DocumentSnapshot): DeliveryEntity? = runCatching {
        DeliveryEntity(
            id = doc.id,
            driverId = doc.getString(DRIVER_ID) ?: return null,
            orderNumber = (doc.getLong("orderNumber") ?: return null).toInt(),
            recipient = doc.getString("recipient") ?: return null,
            address = doc.getString("address") ?: return null,
            neighborhood = doc.getString("neighborhood").orEmpty(),
            city = doc.getString("city").orEmpty(),
            packageType = doc.getString("packageType").orEmpty(),
            note = doc.getString("note"),
            latitude = doc.getDouble("latitude") ?: return null,
            longitude = doc.getDouble("longitude") ?: return null,
            status = doc.getString(STATUS) ?: "PENDING",
            legPath = doc.getString("legPath").orEmpty(),
            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
            isSynced = true,
            updatedAt = doc.getLong(UPDATED_AT) ?: System.currentTimeMillis()
        )
    }.getOrNull()
}
