package br.edu.ifsp.ifrota.data.repository

import br.edu.ifsp.ifrota.core.UserRole
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class RoleRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    /**
     * Observa o campo "role" do documento do usuário na coleção "drivers". Emite null
     * enquanto não há resposta confiável (erro de leitura ou documento ainda inexistente);
     * nesse caso o AuthManager assume DRIVER após o timeout.
     */
    fun observeRole(userId: String): Flow<UserRole?> = callbackFlow {
        val registration = firestore.collection("drivers").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(UserRole.fromFirestore(snapshot.get(UserRole.FIELD)))
            }
        awaitClose { registration.remove() }
    }
}
