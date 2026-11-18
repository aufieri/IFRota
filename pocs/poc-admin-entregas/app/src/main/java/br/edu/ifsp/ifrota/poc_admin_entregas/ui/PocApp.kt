package br.edu.ifsp.ifrota.poc_admin_entregas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import br.edu.ifsp.ifrota.poc_admin_entregas.core.RoleRepository
import br.edu.ifsp.ifrota.poc_admin_entregas.core.UserRole
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val ROLE_TIMEOUT_MILLIS = 8_000L

/** Login (Firebase Auth) e, depois dele, a tela do papel do usuário: administrador ou motorista. */
@Composable
fun PocApp() {
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            val auth = remember { FirebaseAuth.getInstance() }
            var user by remember { mutableStateOf(auth.currentUser) }
            DisposableEffect(auth) {
                val listener = FirebaseAuth.AuthStateListener { user = it.currentUser }
                auth.addAuthStateListener(listener)
                onDispose { auth.removeAuthStateListener(listener) }
            }

            val current = user
            if (current == null) {
                LoginScreen(auth)
            } else {
                // O papel vem do servidor (documento do usuário no Firestore), nunca do Room.
                val role by produceState<UserRole?>(initialValue = null, current.uid) {
                    // Sem resposta do servidor, assume o papel de menor privilégio.
                    launch {
                        delay(ROLE_TIMEOUT_MILLIS)
                        if (value == null) value = UserRole.DRIVER
                    }
                    RoleRepository().observeRole(current.uid).collect { value = it }
                }
                val email = current.email.orEmpty()
                when (role) {
                    null -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    UserRole.ADMIN -> AdminScreens(current.uid, email, onLogout = { auth.signOut() })
                    UserRole.DRIVER -> DriverScreen(current.uid, email, onLogout = { auth.signOut() })
                }
            }
        }
    }
}

@Composable
private fun LoginScreen(auth: FirebaseAuth) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        Text("PoC · Admin e entregas", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Entre com uma conta já cadastrada no IFRota (administrador ou motorista).",
            style = MaterialTheme.typography.bodyMedium
        )
        OutlinedTextField(
            value = email, onValueChange = { email = it; error = null },
            label = { Text("E-mail") }, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = password, onValueChange = { password = it; error = null },
            label = { Text("Senha") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth()
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = {
                loading = true
                scope.launch {
                    try {
                        auth.signInWithEmailAndPassword(email.trim(), password).await()
                    } catch (e: Exception) {
                        error = "E-mail ou senha incorretos."
                    } finally {
                        loading = false
                    }
                }
            },
            enabled = !loading && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (loading) "Entrando…" else "Entrar") }
    }
}
