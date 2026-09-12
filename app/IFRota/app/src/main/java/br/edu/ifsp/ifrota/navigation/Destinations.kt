package br.edu.ifsp.ifrota.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Person
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
    VEHICLES("veiculos", "Veículos", Icons.Outlined.LocalShipping, Icons.Filled.LocalShipping),
    PROFILE("perfil", "Perfil", Icons.Outlined.Person, Icons.Filled.Person)
}
