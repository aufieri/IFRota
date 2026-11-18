package br.edu.ifsp.ifrota.core

/**
 * Papel do usuário, lido do campo `role` em `drivers/{uid}` no Firestore (ver
 * `RoleRepository`). O papel não existe no Room nem é editável pelo app: as regras do
 * Firestore só aceitam que ele seja definido fora do aplicativo (console do Firebase).
 */
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
