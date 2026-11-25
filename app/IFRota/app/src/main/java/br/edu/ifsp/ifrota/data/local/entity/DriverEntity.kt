package br.edu.ifsp.ifrota.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude

/**
 * Perfil do entregador. O [id] é o UID do usuário no Firebase Auth, de modo que cada
 * conta tem exatamente um perfil e ele é carregado direto pelo UID após o login.
 */
@Entity(tableName = "drivers")
data class DriverEntity(
    @PrimaryKey
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val cnh: String = "",
    val phone: String = "",

    /** Caminho local da foto de perfil — não sincronizado com o Firestore. */
    @get:Exclude
    val photoUri: String = "",

    val isSynced: Boolean = false,

    val isDeleted: Boolean = false,

    val updatedAt: Long = System.currentTimeMillis()
)
