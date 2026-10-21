package br.edu.ifsp.ifrota.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Route
import androidx.compose.ui.graphics.vector.ImageVector


object AuthRoutes {
    const val LOGIN = "login"
    const val SIGN_UP = "signup"
    const val MAIN = "main"
}
enum class MainTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    HOME("inicio", "Início", Icons.Outlined.Home, Icons.Filled.Home),
    DELIVERIES("entregas", "Entregas", Icons.Outlined.Inventory2, Icons.Filled.Inventory2),
    ROUTES("rotas", "Rotas", Icons.Outlined.Route, Icons.Filled.Route),
    VEHICLES("veiculos", "Veículos", Icons.Outlined.LocalShipping, Icons.Filled.LocalShipping),
    PROFILE("perfil", "Perfil", Icons.Outlined.Person, Icons.Filled.Person)
}

/** Telas da área administrativa. */
object AdminRoutes {
    const val DRIVERS = "admin/motoristas"
    const val DRIVER_ID = "driverId"
    const val DRIVER_DETAIL = "admin/motoristas/{$DRIVER_ID}"

    fun driverDetail(id: String) = "admin/motoristas/$id"
}

/** Telas internas, abertas a partir das abas e sem a barra inferior. */
object MainRoutes {
    const val DELIVERY_ID = "id"
    const val DELIVERY_DETAIL = "entregas/{$DELIVERY_ID}"

    fun deliveryDetail(id: String) = "entregas/$id"
}
