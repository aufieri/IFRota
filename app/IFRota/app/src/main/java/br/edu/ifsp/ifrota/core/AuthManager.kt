package br.edu.ifsp.ifrota.core

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.edu.ifsp.ifrota.views.HomeView
import br.edu.ifsp.ifrota.views.LoginView
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AuthManager() {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()

    val start = if (auth.currentUser != null) "home" else "login"

    NavHost(navController = navController, startDestination = start) {
        composable("login") {
            LoginView (
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            HomeView (
                onLogout = {
                    auth.signOut()
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
    }
}