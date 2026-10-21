package br.edu.ifsp.ifrota.data.sync

import android.util.Log
import br.edu.ifsp.ifrota.data.delivery.DeliveryDocument
import br.edu.ifsp.ifrota.data.local.dao.DeliveryDao
import br.edu.ifsp.ifrota.data.local.dao.DriverDao
import br.edu.ifsp.ifrota.data.local.dao.VehicleDao
import br.edu.ifsp.ifrota.data.local.entity.DriverEntity
import br.edu.ifsp.ifrota.data.local.entity.VehicleEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "SyncManager"
class SyncManager(
    private val userId: String,
    private val driverDao: DriverDao,
    private val vehicleDao: VehicleDao,
    private val deliveryDao: DeliveryDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var driverListener: ListenerRegistration? = null
    private var vehiclesListener: ListenerRegistration? = null
    private var deliveriesListener: ListenerRegistration? = null
    private var pendingProfileJob: Job? = null

    suspend fun syncAll() {
        pushDrivers()
        pushVehicles()
        pushDeliveries()
    }

    suspend fun pushDrivers() {
        driverDao.getUnsynced().forEach { driver ->
            try {
                val docRef = firestore.collection("drivers").document(driver.id)
                if (driver.isDeleted) {
                    docRef.delete().await()
                    driverDao.purgeIfDeleted(driver.id)
                } else {
                    // merge: o campo `role` do documento é definido fora do app e não pode ser apagado aqui.
                    docRef.set(driver, SetOptions.merge()).await()
                    driverDao.markAsSynced(driver.id)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Falha ao enviar driver ${driver.id}", e)
            }
        }
    }

    suspend fun pushVehicles() {
        vehicleDao.getUnsynced().forEach { vehicle ->
            try {
                val docRef = firestore.collection("vehicles").document(vehicle.id)
                if (vehicle.isDeleted) {
                    docRef.delete().await()
                    vehicleDao.purgeIfDeleted(vehicle.id)
                } else {
                    docRef.set(vehicle).await()
                    vehicleDao.markAsSynced(vehicle.id)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Falha ao enviar vehicle ${vehicle.id}", e)
            }
        }
    }

    /**
     * Envia ao Firestore só o que o motorista pode alterar numa entrega: o status. O restante
     * do documento pertence ao administrador (as regras do Firestore também garantem isso).
     */
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
        if (pendingProfileJob == null) {
            // O cadastro grava o perfil no Room logo depois de criar a conta, quando esta
            // sessão já começou: envia assim que ele aparecer como pendente.
            pendingProfileJob = scope.launch {
                driverDao.observeUnsynced().collect { if (it.isNotEmpty()) pushDrivers() }
            }
        }
        if (driverListener == null) {
            driverListener = firestore.collection("drivers").document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        Log.w(TAG, "Listener do perfil falhou", error)
                        return@addSnapshotListener
                    }
                    scope.launch { applyDriverSnapshot(snapshot) }
                }
        }
        if (vehiclesListener == null) {
            vehiclesListener = firestore.collection("vehicles")
                .whereEqualTo("ownerId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        Log.w(TAG, "Listener de veículos falhou", error)
                        return@addSnapshotListener
                    }
                    snapshot.documentChanges.forEach { change ->
                        scope.launch { applyVehicleChange(change) }
                    }
                }
        }
        if (deliveriesListener == null) {
            deliveriesListener = firestore.collection(DeliveryDocument.COLLECTION)
                .whereEqualTo(DeliveryDocument.DRIVER_ID, userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        Log.w(TAG, "Listener de entregas falhou", error)
                        return@addSnapshotListener
                    }
                    snapshot.documentChanges.forEach { change ->
                        scope.launch { applyDeliveryChange(change) }
                    }
                }
        }
    }

    fun stopListening() {
        pendingProfileJob?.cancel()
        pendingProfileJob = null
        driverListener?.remove()
        vehiclesListener?.remove()
        deliveriesListener?.remove()
        driverListener = null
        vehiclesListener = null
        deliveriesListener = null
    }

    fun close() {
        stopListening()
        scope.cancel()
    }

    /**
     * Conta que existe no Firebase Auth mas nunca teve o perfil gravado (cadastro interrompido
     * antes do envio): recria um perfil mínimo com os dados da conta, que então sobe ao Firestore.
     */
    private suspend fun createMissingProfile() {
        if (driverDao.getById(userId) != null) return
        val user = auth.currentUser?.takeIf { it.uid == userId } ?: return
        val email = user.email.orEmpty()
        driverDao.upsert(
            DriverEntity(
                id = userId,
                name = user.displayName?.takeIf { it.isNotBlank() } ?: email.substringBefore('@'),
                email = email,
                isSynced = false,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private suspend fun applyDriverSnapshot(snapshot: DocumentSnapshot) {
        if (!snapshot.exists()) {
            createMissingProfile()
            return
        }

        val remote = snapshot.toObject<DriverEntity>()
            ?.copy(id = snapshot.id, isSynced = true)
            ?: return
        val local = driverDao.getById(snapshot.id)

        val localPendingIsNewer = local != null && !local.isSynced && local.updatedAt > remote.updatedAt
        if (!localPendingIsNewer) {
            driverDao.upsert(remote)
        }
    }

    private suspend fun applyVehicleChange(change: DocumentChange) {
        val id = change.document.id

        if (change.type == DocumentChange.Type.REMOVED) {
            val local = vehicleDao.getById(id) ?: return
            if (local.isSynced) vehicleDao.upsert(local.copy(isDeleted = true, isSynced = true))
            return
        }

        val remote = change.document.toObject<VehicleEntity>().copy(id = id, isSynced = true)
        val local = vehicleDao.getById(id)

        val localPendingIsNewer = local != null && !local.isSynced && local.updatedAt > remote.updatedAt
        if (!localPendingIsNewer) {
            vehicleDao.upsert(remote)
        }
    }

    private suspend fun applyDeliveryChange(change: DocumentChange) {
        val id = change.document.id

        if (change.type == DocumentChange.Type.REMOVED) {
            deliveryDao.deleteById(id)
            return
        }

        val remote = DeliveryDocument.fromSnapshot(change.document) ?: return
        val local = deliveryDao.getById(id)

        val localPendingIsNewer = local != null && !local.isSynced && local.updatedAt > remote.updatedAt
        if (!localPendingIsNewer) {
            deliveryDao.upsert(remote)
        }
    }
}
