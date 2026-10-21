package br.edu.ifsp.ifrota.core

import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.edu.ifsp.ifrota.navigation.AuthRoutes
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import br.edu.ifsp.ifrota.ui.theme.Green600
import br.edu.ifsp.ifrota.ui.theme.IFRotaTheme
import br.edu.ifsp.ifrota.ui.theme.Surface2
import br.edu.ifsp.ifrota.views.AdminScaffold
import br.edu.ifsp.ifrota.views.LoginView
import br.edu.ifsp.ifrota.views.MainScaffold
import br.edu.ifsp.ifrota.views.SignUpView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val ROLE_TIMEOUT_MILLIS = 8_000L

@Composable
fun AuthManager() {
    val auth = remember { FirebaseAuth.getInstance() }
    var currentUser by remember { mutableStateOf(auth.currentUser) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { currentUser = it.currentUser }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    IFRotaTheme {
        val user = currentUser
        if (user == null) {
            AuthNavHost()
        } else {
            // O papel vem do servidor (documento do usuário no Firestore), nunca do Room.
            val role by produceState<UserRole?>(initialValue = null, user.uid) {
                // Sem resposta do servidor (offline, sem cache), assume o papel de menor privilégio.
                launch {
                    delay(ROLE_TIMEOUT_MILLIS)
                    if (value == null) value = UserRole.DRIVER
                }
                RoleRepository().observeRole(user.uid).collect { value = it }
            }
            when (role) {
                null -> Box(Modifier.fillMaxSize().background(Surface2), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Green600)
                }
                UserRole.ADMIN -> AdminScaffold(userId = user.uid, accountEmail = user.email.orEmpty())
                UserRole.DRIVER -> MainScaffold(userId = user.uid, accountEmail = user.email.orEmpty())
            }
        }
    }
}

@Composable
private fun AuthNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = AuthRoutes.LOGIN) {
        composable(AuthRoutes.LOGIN) {
            LoginView(
                onNavigateToSignUp = { navController.navigate(AuthRoutes.SIGN_UP) }
            )
        }
        composable(AuthRoutes.SIGN_UP) {
            SignUpView(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
