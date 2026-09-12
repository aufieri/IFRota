package br.edu.ifsp.ifrota.views

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.edu.ifsp.ifrota.core.clearLocalData
import br.edu.ifsp.ifrota.core.rememberUserSession
import br.edu.ifsp.ifrota.navigation.MainTab
import br.edu.ifsp.ifrota.ui.components.IFRotaBottomBar
import br.edu.ifsp.ifrota.ui.theme.Surface2
import br.edu.ifsp.ifrota.ui.viewmodel.DashboardViewModel
import br.edu.ifsp.ifrota.ui.viewmodel.DashboardViewModelFactory
import br.edu.ifsp.ifrota.ui.viewmodel.ProfileViewModel
import br.edu.ifsp.ifrota.ui.viewmodel.ProfileViewModelFactory
import br.edu.ifsp.ifrota.ui.viewmodel.VehiclesViewModel
import br.edu.ifsp.ifrota.ui.viewmodel.VehiclesViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun MainScaffold(userId: String, accountEmail: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = rememberUserSession(userId)
    val navController = rememberNavController()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val tabs = MainTab.entries

    Scaffold(
        containerColor = Surface2,
        bottomBar = {
            IFRotaBottomBar(
                tabs = tabs,
                currentRoute = currentRoute,
                onTabSelected = { tab ->
                    if (tab.route != currentRoute) {
                        navController.navigate(tab.route) {
                            popUpTo(MainTab.HOME.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MainTab.HOME.route,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            composable(MainTab.HOME.route) {
                val dashboardViewModel: DashboardViewModel = viewModel(
                    factory = DashboardViewModelFactory(
                        session.driverRepository,
                        session.vehicleRepository
                    )
                )
                DashboardView(
                    viewModel = dashboardViewModel,
                    accountEmail = accountEmail,
                    onOpenProfile = { navController.navigateToTab(MainTab.PROFILE) },
                    onOpenVehicles = { navController.navigateToTab(MainTab.VEHICLES) }
                )
            }

            composable(MainTab.VEHICLES.route) {
                val vehiclesViewModel: VehiclesViewModel = viewModel(
                    factory = VehiclesViewModelFactory(session.vehicleRepository)
                )
                VehiclesView(viewModel = vehiclesViewModel)
            }

            composable(MainTab.PROFILE.route) {
                val profileViewModel: ProfileViewModel = viewModel(
                    factory = ProfileViewModelFactory(
                        session.driverRepository,
                        session.vehicleRepository,
                        accountEmail
                    )
                )
                ProfileView(
                    viewModel = profileViewModel,
                    accountEmail = accountEmail,
                    onLogout = {
                        scope.launch {
                            clearLocalData(context)
                            FirebaseAuth.getInstance().signOut()
                        }
                    }
                )
            }
        }
    }
}

private fun androidx.navigation.NavController.navigateToTab(tab: MainTab) {
    navigate(tab.route) {
        popUpTo(MainTab.HOME.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
