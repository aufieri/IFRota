package br.edu.ifsp.ifrota.views

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import br.edu.ifsp.ifrota.data.tracking.DriverTracker
import br.edu.ifsp.ifrota.data.tracking.TrackingRepository
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.edu.ifsp.ifrota.core.clearLocalData
import br.edu.ifsp.ifrota.core.rememberUserSession
import br.edu.ifsp.ifrota.navigation.MainTab
import br.edu.ifsp.ifrota.navigation.MainRoutes
import br.edu.ifsp.ifrota.notification.GeofenceNotifier
import br.edu.ifsp.ifrota.ui.components.IFRotaBottomBar
import br.edu.ifsp.ifrota.ui.theme.Surface2
import br.edu.ifsp.ifrota.ui.viewmodel.DashboardViewModel
import br.edu.ifsp.ifrota.ui.viewmodel.DashboardViewModelFactory
import br.edu.ifsp.ifrota.ui.viewmodel.DeliveryViewModel
import br.edu.ifsp.ifrota.ui.viewmodel.DeliveryViewModelFactory
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

    // Entradas e saídas de perímetro viram notificações do sistema (geofencing).
    val geofenceNotifier = remember(context) { GeofenceNotifier(context.applicationContext) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (!geofenceNotifier.canNotify() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Rastreamento para o administrador: posição em locations/{uid} e eventos de perímetro.
    val trackingRepository = remember { TrackingRepository() }
    val tracker = remember(userId) { DriverTracker(userId, trackingRepository) }

    // Um único andamento do dia para Início, Entregas e Rotas (fora do NavHost, sobrevive
    // à troca de abas). A chave separa contas diferentes no mesmo aparelho.
    val deliveryViewModel: DeliveryViewModel = viewModel(
        key = "deliveries-$userId",
        factory = DeliveryViewModelFactory(
            session.vehicleRepository,
            session.deliveryRepository,
            onGeofenceEvent = { event ->
                geofenceNotifier.notify(event)
                // Garante que o evento só sai com a conta dona dele logada.
                if (FirebaseAuth.getInstance().currentUser?.uid == userId) {
                    trackingRepository.publishEvent(userId, event)
                }
            }
        )
    )

    // Publica a posição enquanto a tela principal existir (inclusive com o app em segundo
    // plano). Ao sair da conta, para antes de marcar a saída, para não sobrescrevê-la.
    var tracking by remember { mutableStateOf(true) }
    if (tracking) {
        LaunchedEffect(tracker, deliveryViewModel) { tracker.run(deliveryViewModel.state) }
    }

    Scaffold(
        containerColor = Surface2,
        bottomBar = {
            // Telas internas (como o detalhe da entrega) ocupam a tela toda, como no protótipo.
            if (tabs.any { it.route == currentRoute }) {
                IFRotaBottomBar(
                    tabs = tabs,
                    currentRoute = currentRoute,
                    onTabSelected = { tab ->
                        if (tab.route != currentRoute) navController.navigateToTab(tab)
                    }
                )
            }
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
                    deliveryViewModel = deliveryViewModel,
                    accountEmail = accountEmail,
                    onOpenProfile = { navController.navigateToTab(MainTab.PROFILE) },
                    onOpenVehicles = { navController.navigateToTab(MainTab.VEHICLES) },
                    onOpenDelivery = { navController.navigate(MainRoutes.deliveryDetail(it.id)) },
                    onOpenRoute = { navController.navigateToTab(MainTab.ROUTES) }
                )
            }

            composable(MainTab.DELIVERIES.route) {
                DeliveriesView(
                    viewModel = deliveryViewModel,
                    onOpenDelivery = { navController.navigate(MainRoutes.deliveryDetail(it.id)) }
                )
            }

            composable(MainRoutes.DELIVERY_DETAIL) { entry ->
                DeliveryDetailView(
                    viewModel = deliveryViewModel,
                    deliveryId = entry.arguments?.getString(MainRoutes.DELIVERY_ID).orEmpty(),
                    onBack = { navController.popBackStack() },
                    onOpenRoute = {
                        // Fecha o detalhe antes: aberto a partir da aba Rotas, ele faz parte da
                        // pilha salva dela, e restaurar a aba traria o próprio detalhe de volta.
                        navController.popBackStack()
                        navController.navigateToTab(MainTab.ROUTES)
                    }
                )
            }

            composable(MainTab.ROUTES.route) {
                RouteView(
                    viewModel = deliveryViewModel,
                    onOpenVehicles = { navController.navigateToTab(MainTab.VEHICLES) },
                    onOpenDelivery = { navController.navigate(MainRoutes.deliveryDetail(it.id)) }
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
                        tracking = false
                        scope.launch {
                            trackingRepository.markSignedOut(userId)
                            clearLocalData(context)
                            FirebaseAuth.getInstance().signOut()
                        }
                    }
                )
            }
        }
    }
}

private fun NavController.navigateToTab(tab: MainTab) {
    navigate(tab.route) {
        popUpTo(MainTab.HOME.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
