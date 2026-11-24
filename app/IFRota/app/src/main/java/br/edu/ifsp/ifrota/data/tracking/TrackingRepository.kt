package br.edu.ifsp.ifrota.data.tracking

import android.util.Log
import br.edu.ifsp.ifrota.data.geofencing.GeofenceEvent
import br.edu.ifsp.ifrota.data.geofencing.GeofenceTransition
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private const val TAG = "TrackingRepository"

/**
 * Rastreamento em tempo real pelo Firestore:
 * - `locations/{driverId}`: última posição de cada entregador (escrita só por ele);
 * - `geofenceEvents`: entradas e saídas de perímetro (criadas pelo entregador).
 * O administrador lê as duas coleções (ver firestore.rules).
 */
data class AccountInfo(val name: String, val isAdmin: Boolean)

class TrackingRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    // ---- Lado do entregador ----

    /** Sobrescreve a posição do entregador. Sem conexão, o Firestore guarda e envia depois. */
    fun publishLocation(location: LiveLocation) {
        firestore.collection(LOCATIONS).document(location.driverId)
            .set(location.toMap())
            .addOnFailureListener { Log.w(TAG, "Falha ao publicar posição", it) }
    }

    /**
     * Ao sair da conta, marca a posição como encerrada: o admin continua vendo onde o
     * entregador estava, mas como offline, em vez de "parado" e online.
     */
    suspend fun markSignedOut(driverId: String) {
        try {
            firestore.collection(LOCATIONS).document(driverId)
                .update(mapOf("signedOut" to true, "updatedAt" to System.currentTimeMillis()))
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao marcar saída do entregador", e)
        }
    }

    fun publishEvent(driverId: String, event: GeofenceEvent) {
        firestore.collection(EVENTS)
            .add(
                mapOf(
                    "driverId" to driverId,
                    "plate" to event.plate,
                    "fenceId" to event.geofence.id,
                    "fenceName" to event.geofence.name,
                    "transition" to event.transition.name,
                    "timestampMillis" to event.timestampMillis
                )
            )
            .addOnFailureListener { Log.w(TAG, "Falha ao registrar evento de perímetro", it) }
    }

    // ---- Lado do administrador ----

    fun observeLocations(): Flow<List<LiveLocation>> = callbackFlow {
        val registration = firestore.collection(LOCATIONS).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.documents.orEmpty().mapNotNull(::locationOf))
        }
        awaitClose { registration.remove() }
    }

    /** Eventos mais recentes primeiro. */
    fun observeRecentEvents(limit: Long = 30): Flow<List<FenceEventRecord>> = callbackFlow {
        val registration = firestore.collection(EVENTS)
            .orderBy("timestampMillis", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().mapNotNull(::eventOf))
            }
        awaitClose { registration.remove() }
    }

    /** Contas cadastradas em `drivers` (id → nome e se é administrador). */
    fun observeAccounts(): Flow<Map<String, AccountInfo>> = callbackFlow {
        val registration = firestore.collection("drivers").addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(
                snapshot?.documents.orEmpty().associate { doc ->
                    doc.id to AccountInfo(
                        name = doc.getString("name")?.takeIf { it.isNotBlank() }
                            ?: doc.getString("email")?.substringBefore('@')
                            ?: "Entregador",
                        isAdmin = doc.getString("role").equals("admin", ignoreCase = true)
                    )
                }
            )
        }
        awaitClose { registration.remove() }
    }

    /**
     * Apaga a posição publicada por esta conta. Usado quando ela é administradora: se antes
     * de receber o papel ela entrou como entregador, a posição antiga não pode ficar na frota.
     */
    suspend fun deleteOwnLocation(userId: String) {
        try {
            firestore.collection(LOCATIONS).document(userId).delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao apagar posição antiga do administrador", e)
        }
    }

    private fun LiveLocation.toMap(): Map<String, Any?> = mapOf(
        "plate" to plate,
        "latitude" to latitude,
        "longitude" to longitude,
        "heading" to heading.toDouble(),
        "speedKmh" to speedKmh,
        "phase" to phase.name,
        "deliveryId" to deliveryId,
        "deliveryOrder" to deliveryOrder,
        "deliveryRecipient" to deliveryRecipient,
        "deliveryLatitude" to deliveryLatitude,
        "deliveryLongitude" to deliveryLongitude,
        "doneCount" to doneCount,
        "totalDeliveries" to totalDeliveries,
        "updatedAt" to updatedAt,
        "signedOut" to signedOut
    )

    private fun locationOf(doc: DocumentSnapshot): LiveLocation? = runCatching {
        LiveLocation(
            driverId = doc.id,
            plate = doc.getString("plate").orEmpty(),
            latitude = doc.getDouble("latitude") ?: return null,
            longitude = doc.getDouble("longitude") ?: return null,
            heading = (doc.getDouble("heading") ?: 0.0).toFloat(),
            speedKmh = (doc.getLong("speedKmh") ?: 0L).toInt(),
            phase = runCatching { TrackingPhase.valueOf(doc.getString("phase").orEmpty()) }
                .getOrDefault(TrackingPhase.IDLE),
            deliveryId = doc.getString("deliveryId"),
            deliveryOrder = doc.getLong("deliveryOrder")?.toInt(),
            deliveryRecipient = doc.getString("deliveryRecipient"),
            deliveryLatitude = doc.getDouble("deliveryLatitude"),
            deliveryLongitude = doc.getDouble("deliveryLongitude"),
            doneCount = (doc.getLong("doneCount") ?: 0L).toInt(),
            totalDeliveries = (doc.getLong("totalDeliveries") ?: 0L).toInt(),
            updatedAt = doc.getLong("updatedAt") ?: 0L,
            signedOut = doc.getBoolean("signedOut") ?: false
        )
    }.getOrNull()

    private fun eventOf(doc: DocumentSnapshot): FenceEventRecord? = runCatching {
        FenceEventRecord(
            id = doc.id,
            driverId = doc.getString("driverId") ?: return null,
            plate = doc.getString("plate").orEmpty(),
            fenceId = doc.getString("fenceId").orEmpty(),
            fenceName = doc.getString("fenceName").orEmpty(),
            transition = GeofenceTransition.valueOf(doc.getString("transition") ?: return null),
            timestampMillis = doc.getLong("timestampMillis") ?: return null
        )
    }.getOrNull()

    private companion object {
        const val LOCATIONS = "locations"
        const val EVENTS = "geofenceEvents"
    }
}
