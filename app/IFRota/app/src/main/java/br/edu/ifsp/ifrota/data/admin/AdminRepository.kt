package br.edu.ifsp.ifrota.data.admin

import android.util.Log
import br.edu.ifsp.ifrota.core.UserRole
import br.edu.ifsp.ifrota.data.delivery.DeliveryBatchBuilder
import br.edu.ifsp.ifrota.data.delivery.DeliveryDocument
import br.edu.ifsp.ifrota.data.delivery.DeliveryPlanner
import br.edu.ifsp.ifrota.data.local.entity.DeliveryEntity
import br.edu.ifsp.ifrota.data.routing.OsrmRouteProvider
import br.edu.ifsp.ifrota.data.routing.RouteProvider
import br.edu.ifsp.ifrota.data.routing.RouteUnavailableException
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Motorista como o administrador o vê na listagem. */
data class AdminDriver(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val cnh: String
)

sealed interface CreateDeliveriesResult {
    data class Created(val count: Int) : CreateDeliveriesResult

    /** O mesmo lote já havia sido gravado (clique repetido ou nova tentativa após falha de rede). */
    data class AlreadyCreated(val count: Int) : CreateDeliveriesResult

    data object DriverNotFound : CreateDeliveriesResult
    data object NoAddressesAvailable : CreateDeliveriesResult
    data class Failure(val message: String) : CreateDeliveriesResult
}

/** O que a tela do administrador precisa; permite testar o ViewModel sem Firestore. */
interface AdminGateway {
    fun observeDrivers(): Flow<List<AdminDriver>>
    fun observeDeliveries(driverId: String): Flow<List<DeliveryEntity>>
    suspend fun createDeliveries(driverId: String, batchId: String, quantity: Int): CreateDeliveriesResult
}

/**
 * Operações do administrador, direto no Firestore. A autorização é das regras do Firestore
 * (`firestore.rules`): mesmo que a tela fosse aberta por um motorista, leituras de outros
 * motoristas e escritas em `deliveries` seriam recusadas pelo servidor.
 */
class AdminRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val routeProvider: RouteProvider = OsrmRouteProvider(),
    private val clock: () -> Long = System::currentTimeMillis
) : AdminGateway {

    /** Todos os motoristas cadastrados (contas administrativas não entram na lista). */
    override fun observeDrivers(): Flow<List<AdminDriver>> = callbackFlow {
        val registration = firestore.collection("drivers")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    Log.w(TAG, "Listener de motoristas falhou", error)
                    close(error)
                    return@addSnapshotListener
                }
                trySend(
                    snapshot.documents
                        .filter { UserRole.fromFirestore(it.get(UserRole.FIELD)) == UserRole.DRIVER }
                        .map { it.toAdminDriver() }
                        .sortedBy { it.name.lowercase() }
                )
            }
        awaitClose { registration.remove() }
    }

    /** Entregas atribuídas ao motorista, na ordem da fila. */
    override fun observeDeliveries(driverId: String): Flow<List<DeliveryEntity>> = callbackFlow {
        val registration = firestore.collection(DeliveryDocument.COLLECTION)
            .whereEqualTo(DeliveryDocument.DRIVER_ID, driverId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    Log.w(TAG, "Listener de entregas falhou", error)
                    close(error)
                    return@addSnapshotListener
                }
                trySend(
                    snapshot.documents.mapNotNull(DeliveryDocument::fromSnapshot)
                        .sortedWith(compareBy({ it.orderNumber }, { it.createdAt }))
                )
            }
        awaitClose { registration.remove() }
    }

    /**
     * Cria [quantity] entregas de demonstração para [driverId], depois das que ele já tem.
     *
     * - [batchId] identifica a ação do administrador: repetir a chamada com o mesmo valor
     *   (clique duplo, nova tentativa após falha de rede) não cria entregas em dobro.
     * - O trajeto de cada entrega é calculado antes de qualquer escrita; se o serviço de rotas
     *   falhar, nada é gravado.
     * - A gravação é uma transação: ou todas as entregas do lote existem, ou nenhuma.
     * - Entregas anteriores nunca são alteradas.
     */
    override suspend fun createDeliveries(
        driverId: String,
        batchId: String,
        quantity: Int
    ): CreateDeliveriesResult {
        try {
            val driverRef = firestore.collection("drivers").document(driverId)
            val driver = driverRef.get().await()
            if (!driver.exists() || UserRole.fromFirestore(driver.get(UserRole.FIELD)) != UserRole.DRIVER) {
                return CreateDeliveriesResult.DriverNotFound
            }

            val existing = firestore.collection(DeliveryDocument.COLLECTION)
                .whereEqualTo(DeliveryDocument.DRIVER_ID, driverId)
                .get().await()
                .documents.mapNotNull(DeliveryDocument::fromSnapshot)
                .sortedWith(compareBy({ it.orderNumber }, { it.createdAt }))

            val plan = when (val p = DeliveryPlanner.plan(existing, batchId, quantity)) {
                is DeliveryPlanner.Plan.AlreadyCreated -> return CreateDeliveriesResult.AlreadyCreated(p.count)
                DeliveryPlanner.Plan.NoAddressesAvailable -> return CreateDeliveriesResult.NoAddressesAvailable
                is DeliveryPlanner.Plan.Ready -> p
            }
            val stops = plan.stops

            // Cada trecho parte da parada anterior da fila (ou do CD, na primeira entrega).
            var from = plan.start
            val legs = stops.map { stop ->
                val to = stop.address.latitude to stop.address.longitude
                routeProvider.route(from, to).also { from = to }
            }

            val deliveries = DeliveryBatchBuilder.build(
                driverId = driverId,
                batchId = batchId,
                stops = stops,
                legs = legs,
                firstOrder = plan.firstOrder,
                now = clock()
            )

            firestore.runTransaction { tx ->
                if (!tx.get(driverRef).exists()) {
                    throw DriverRemovedException()
                }
                val refs = deliveries.map {
                    firestore.collection(DeliveryDocument.COLLECTION).document(it.id)
                }
                // Outra requisição do mesmo lote terminou primeiro: não sobrescreve nada.
                if (tx.get(refs.first()).exists()) {
                    throw BatchAlreadyWrittenException()
                }
                deliveries.zip(refs).forEach { (delivery, ref) ->
                    tx.set(ref, DeliveryDocument.toMap(delivery))
                }
            }.await()

            return CreateDeliveriesResult.Created(deliveries.size)
        } catch (e: Exception) {
            // O SDK pode devolver a exceção da transação embrulhada em outra.
            if (e.hasInChain<BatchAlreadyWrittenException>()) return CreateDeliveriesResult.AlreadyCreated(0)
            if (e.hasInChain<DriverRemovedException>()) return CreateDeliveriesResult.DriverNotFound
            Log.w(TAG, "Falha ao criar entregas de $driverId", e)
            return CreateDeliveriesResult.Failure(e.toAdminMessage())
        }
    }

    private class DriverRemovedException : Exception()
    private class BatchAlreadyWrittenException : Exception()

    private inline fun <reified T : Throwable> Throwable.hasInChain(): Boolean =
        generateSequence(this) { it.cause }.any { it is T }

    private fun Exception.toAdminMessage(): String = when {
        this is RouteUnavailableException -> message ?: "Serviço de rotas indisponível."
        this is FirebaseFirestoreException && code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "Sem permissão para criar entregas. Esta conta não é administradora."
        this is FirebaseFirestoreException && code == FirebaseFirestoreException.Code.UNAVAILABLE ->
            "Sem conexão. Verifique a internet e tente novamente."
        else -> "Não foi possível criar as entregas. Tente novamente."
    }

    private fun DocumentSnapshot.toAdminDriver() = AdminDriver(
        id = id,
        name = getString("name").orEmpty(),
        email = getString("email").orEmpty(),
        phone = getString("phone").orEmpty(),
        cnh = getString("cnh").orEmpty()
    )

    private companion object {
        const val TAG = "AdminRepository"
    }
}
