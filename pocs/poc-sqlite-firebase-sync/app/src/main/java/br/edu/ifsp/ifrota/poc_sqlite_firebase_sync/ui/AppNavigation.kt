package br.edu.ifsp.ifrota.poc_sqlite_firebase_sync.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.edu.ifsp.ifrota.poc_sqlite_firebase_sync.ui.screens.DriverScreen
import br.edu.ifsp.ifrota.poc_sqlite_firebase_sync.ui.screens.HomeScreen
import br.edu.ifsp.ifrota.poc_sqlite_firebase_sync.ui.screens.VehicleScreen
import br.edu.ifsp.ifrota.poc_sqlite_firebase_sync.ui.theme.POCFirebaseAuthTheme

@Composable
fun AppNavigation() {
    POCFirebaseAuthTheme {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                HomeScreen(
                    onNavigateToDrivers = { navController.navigate("drivers") },
                    onNavigateToVehicles = { navController.navigate("vehicles") }
                )
            }
            composable("drivers") {
                DriverScreen(onBack = { navController.popBackStack() })
            }
            composable("vehicles") {
                VehicleScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
