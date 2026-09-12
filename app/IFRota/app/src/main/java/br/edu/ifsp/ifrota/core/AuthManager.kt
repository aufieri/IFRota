package br.edu.ifsp.ifrota.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.edu.ifsp.ifrota.navigation.AuthRoutes
import br.edu.ifsp.ifrota.ui.theme.IFRotaTheme
import br.edu.ifsp.ifrota.views.LoginView
import br.edu.ifsp.ifrota.views.MainScaffold
import br.edu.ifsp.ifrota.views.SignUpView
import com.google.firebase.auth.FirebaseAuth
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
            MainScaffold(
                userId = user.uid,
                accountEmail = user.email.orEmpty()
            )
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
