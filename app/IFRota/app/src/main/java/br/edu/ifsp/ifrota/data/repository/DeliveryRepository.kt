package br.edu.ifsp.ifrota.data.repository

import br.edu.ifsp.ifrota.data.local.dao.DeliveryDao
import br.edu.ifsp.ifrota.data.local.entity.DeliveryEntity
import br.edu.ifsp.ifrota.data.sync.SyncManager
import kotlinx.coroutines.flow.Flow

/**
 * Entregas do motorista logado. Só leem e alteram as entregas cujo `driverId` é o do
 * usuário da sessão; quem cria entregas é o administrador (ver `AdminRepository`).
 */
class DeliveryRepository(
    private val userId: String,
    private val deliveryDao: DeliveryDao,
    private val syncManager: SyncManager
) {

    fun observeMyDeliveries(): Flow<List<DeliveryEntity>> = deliveryDao.observeByDriver(userId)

    /** Conclui a entrega e a envia ao Firestore (ou deixa pendente até a conexão voltar). */
    suspend fun markDone(deliveryId: String): Boolean {
        val changed = deliveryDao.markDone(deliveryId, userId, System.currentTimeMillis()) > 0
        if (changed) syncManager.pushDeliveries()
        return changed
    }
}
