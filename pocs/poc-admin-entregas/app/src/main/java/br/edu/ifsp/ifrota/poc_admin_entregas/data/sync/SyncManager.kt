package br.edu.ifsp.ifrota.poc_admin_entregas.data.sync

import android.util.Log
import br.edu.ifsp.ifrota.poc_admin_entregas.data.delivery.DeliveryDocument
import br.edu.ifsp.ifrota.poc_admin_entregas.data.local.dao.DeliveryDao
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "SyncManager"

/**
 * Sincronização das entregas do motorista logado (a parte nova do SyncManager do app
 * principal): traz do Firestore só as entregas com `driverId == userId` e envia de volta
 * apenas o status quando o motorista conclui uma.
 */
class SyncManager(
    private val userId: String,
    private val deliveryDao: DeliveryDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var deliveriesListener: ListenerRegistration? = null

    suspend fun syncAll() = pushDeliveries()

    suspend fun pushDeliveries() {
        deliveryDao.getUnsynced().forEach { delivery ->
            try {
                firestore.collection(DeliveryDocument.COLLECTION).document(delivery.id)
                    .update(
                        mapOf(
                            DeliveryDocument.STATUS to delivery.status,
                            DeliveryDocument.UPDATED_AT to delivery.updatedAt
                        )
                    )
                    .await()
                deliveryDao.markAsSynced(delivery.id, delivery.updatedAt)
            } catch (e: Exception) {
                Log.w(TAG, "Falha ao enviar entrega ${delivery.id}", e)
            }
        }
    }

    fun startListening() {
        if (deliveriesListener != null) return
        deliveriesListener = firestore.collection(DeliveryDocument.COLLECTION)
            .whereEqualTo(DeliveryDocument.DRIVER_ID, userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    Log.w(TAG, "Listener de entregas falhou", error)
                    return@addSnapshotListener
                }
                snapshot.documentChanges.forEach { change -> scope.launch { applyChange(change) } }
            }
    }

    fun close() {
        deliveriesListener?.remove()
        deliveriesListener = null
        scope.cancel()
    }

    private suspend fun applyChange(change: DocumentChange) {
        val id = change.document.id
        if (change.type == DocumentChange.Type.REMOVED) {
            deliveryDao.deleteById(id)
            return
        }
        val remote = DeliveryDocument.fromSnapshot(change.document) ?: return
        val local = deliveryDao.getById(id)

        val localPendingIsNewer = local != null && !local.isSynced && local.updatedAt > remote.updatedAt
        if (!localPendingIsNewer) deliveryDao.upsert(remote)
    }
}
