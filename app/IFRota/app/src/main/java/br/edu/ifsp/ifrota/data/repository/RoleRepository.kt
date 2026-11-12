package br.edu.ifsp.ifrota.data.repository

import br.edu.ifsp.ifrota.core.UserRole
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class RoleRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    /** Observa o campo "role" do documento do usuário na coleção "drivers". */
    fun observeRole(userId: String): Flow<UserRole?> = callbackFlow {
        val registration = firestore.collection("drivers").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Offline ou GMS indisponível: fluxo emite null, timeout no AuthManager assume DRIVER.
                    trySend(null)
                    return@addSnapshotListener
                }
                val role = when (snapshot?.getString("role")?.uppercase()) {
                    "ADMIN" -> UserRole.ADMIN
                    "DRIVER" -> UserRole.DRIVER
                    else -> null
                }
                trySend(role)
            }
        awaitClose { registration.remove() }
    }
}
