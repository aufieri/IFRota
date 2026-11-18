package br.edu.ifsp.ifrota.poc_admin_entregas.core

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class UserRole {
    DRIVER,
    ADMIN;

    companion object {
        const val FIELD = "role"
        const val ADMIN_VALUE = "admin"

        /** Qualquer valor ausente ou desconhecido é motorista: só `"admin"` dá acesso administrativo. */
        fun fromFirestore(value: Any?): UserRole =
            if (value == ADMIN_VALUE) ADMIN else DRIVER
    }
}

/**
 * Lê o papel do usuário no documento `drivers/{uid}` do Firestore. O papel não existe no
 * Room nem é editável pelo app: as regras do Firestore só aceitam que ele seja definido
 * fora do aplicativo (console do Firebase), de modo que alterar dados locais não promove
 * ninguém a administrador.
 */
class RoleRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    /** Emite o papel atual e acompanha mudanças; em caso de erro de leitura, emite [UserRole.DRIVER]. */
    fun observeRole(userId: String): Flow<UserRole> = callbackFlow {
        val registration = firestore.collection("drivers").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("RoleRepository", "Não foi possível ler o papel do usuário", error)
                    trySend(UserRole.DRIVER)
                    return@addSnapshotListener
                }
                trySend(UserRole.fromFirestore(snapshot?.get(UserRole.FIELD)))
            }
        awaitClose { registration.remove() }
    }
}
