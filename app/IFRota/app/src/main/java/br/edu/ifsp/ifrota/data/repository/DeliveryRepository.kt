package br.edu.ifsp.ifrota.data.repository

import br.edu.ifsp.ifrota.data.local.dao.DeliveryDao
import br.edu.ifsp.ifrota.data.local.entity.DeliveryEntity
import br.edu.ifsp.ifrota.data.sync.SyncManager
import kotlinx.coroutines.flow.Flow

class DeliveryRepository(
    private val driverId: String,
    private val dao: DeliveryDao,
    private val syncManager: SyncManager
) {
    fun observeMyDeliveries(): Flow<List<DeliveryEntity>> =
        dao.observeForDriver(driverId)

    suspend fun markDone(deliveryId: String) {
        dao.markDone(deliveryId)
        syncManager.pushDeliveries()
    }
}
