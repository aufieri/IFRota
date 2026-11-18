package br.edu.ifsp.ifrota.views

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.edu.ifsp.ifrota.core.clearLocalData
import br.edu.ifsp.ifrota.data.admin.AdminRepository
import br.edu.ifsp.ifrota.navigation.AdminRoutes
import br.edu.ifsp.ifrota.ui.viewmodel.AdminViewModel
import br.edu.ifsp.ifrota.ui.viewmodel.AdminViewModelFactory
import br.edu.ifsp.ifrota.ui.viewmodel.DriversState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * Área do administrador: lista de motoristas e, dentro de cada um, as entregas dele. Só é
 * alcançada quando o papel lido do servidor é administrador (ver `AuthManager`); o que o
 * servidor de fato permite é decidido pelas regras do Firestore, não por esta tela.
 */
@Composable
fun AdminScaffold(userId: String, accountEmail: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()

    val viewModel: AdminViewModel = viewModel(
        key = "admin-$userId",
        factory = AdminViewModelFactory(remember { AdminRepository() })
    )

    NavHost(navController = navController, startDestination = AdminRoutes.DRIVERS) {
        composable(AdminRoutes.DRIVERS) {
            AdminDriversView(
                viewModel = viewModel,
                accountEmail = accountEmail,
                onOpenDriver = { navController.navigate(AdminRoutes.driverDetail(it.id)) },
                onLogout = {
                    scope.launch {
                        clearLocalData(context)
                        FirebaseAuth.getInstance().signOut()
                    }
                }
            )
        }

        composable(
            route = AdminRoutes.DRIVER_DETAIL,
            arguments = listOf(navArgument(AdminRoutes.DRIVER_ID) { type = NavType.StringType })
        ) { entry ->
            val driverId = entry.arguments?.getString(AdminRoutes.DRIVER_ID).orEmpty()
            val drivers by viewModel.drivers.collectAsStateWithLifecycle()
            val driver = (drivers as? DriversState.Ready)?.drivers?.find { it.id == driverId }

            if (driver != null) {
                AdminDriverDetailView(
                    viewModel = viewModel,
                    driver = driver,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
