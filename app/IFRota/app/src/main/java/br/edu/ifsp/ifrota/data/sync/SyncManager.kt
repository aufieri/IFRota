package br.edu.ifsp.ifrota.data.sync

import br.edu.ifsp.ifrota.data.local.entity.DriverEntity
import br.edu.ifsp.ifrota.data.local.entity.VehicleEntity
import br.edu.ifsp.ifrota.data.local.dao.DriverDao
import br.edu.ifsp.ifrota.data.local.dao.VehicleDao
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.tasks.await

class SyncManager(
    private val driverDao: DriverDao,
    private val vehicleDao: VehicleDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun pushDrivers() {
        val pendentes = driverDao.getUnsynced()
        pendentes.forEach { driver ->
            val docRef = firestore.collection("drivers").document(driver.id)
            if (driver.isDeleted) {
                docRef.delete().await()
                driverDao.purgeIfDeleted(driver.id)
            } else {
                docRef.set(driver).await()
                driverDao.markAsSynced(driver.id)
            }
        }
    }

    suspend fun pushVehicles() {
        val pendentes = vehicleDao.getUnsynced()
        pendentes.forEach { vehicle ->
            val docRef = firestore.collection("vehicles").document(vehicle.id)
            if (vehicle.isDeleted) {
                docRef.delete().await()
                vehicleDao.purgeIfDeleted(vehicle.id)
            } else {
                docRef.set(vehicle).await()
                vehicleDao.markAsSynced(vehicle.id)
            }
        }
    }

    fun listenDrivers() {
        firestore.collection("drivers").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            snapshot.documentChanges.forEach { change ->
                val driver = change.document.toObject<DriverEntity>().copy(isSynced = true)
                // roda numa coroutine (ex: dentro de um scope do Repository/ViewModel)
                // driverDao.upsert(driver)
            }
        }
    }

    fun listenVehicles() {
        firestore.collection("vehicles").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            snapshot.documentChanges.forEach { change ->
                val vehicle = change.document.toObject<VehicleEntity>().copy(isSynced = true)
                // vehicleDao.upsert(vehicle)
            }
        }
    }
}