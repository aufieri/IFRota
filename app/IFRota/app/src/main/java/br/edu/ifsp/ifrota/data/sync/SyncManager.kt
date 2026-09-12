package br.edu.ifsp.ifrota.data.sync

import android.util.Log
import br.edu.ifsp.ifrota.data.local.dao.DriverDao
import br.edu.ifsp.ifrota.data.local.dao.VehicleDao
import br.edu.ifsp.ifrota.data.local.entity.DriverEntity
import br.edu.ifsp.ifrota.data.local.entity.VehicleEntity
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.CoroutineScope
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
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var driverListener: ListenerRegistration? = null
    private var vehiclesListener: ListenerRegistration? = null

    suspend fun syncAll() {
        pushDrivers()
        pushVehicles()
    }

    suspend fun pushDrivers() {
        driverDao.getUnsynced().forEach { driver ->
            try {
                val docRef = firestore.collection("drivers").document(driver.id)
                if (driver.isDeleted) {
                    docRef.delete().await()
                    driverDao.purgeIfDeleted(driver.id)
                } else {
                    docRef.set(driver).await()
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

    fun startListening() {
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
    }

    fun stopListening() {
        driverListener?.remove()
        vehiclesListener?.remove()
        driverListener = null
        vehiclesListener = null
    }

    fun close() {
        stopListening()
        scope.cancel()
    }

    private suspend fun applyDriverSnapshot(snapshot: DocumentSnapshot) {
        if (!snapshot.exists()) return

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
}
