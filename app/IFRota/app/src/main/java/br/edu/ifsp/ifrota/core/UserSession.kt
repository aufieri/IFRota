package br.edu.ifsp.ifrota.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import br.edu.ifsp.ifrota.data.local.AppDatabase
import br.edu.ifsp.ifrota.data.repository.DriverRepository
import br.edu.ifsp.ifrota.data.repository.VehicleRepository
import br.edu.ifsp.ifrota.data.sync.SyncManager
import kotlinx.coroutines.launch

/**
 * Reúne banco, sincronização e repositórios de um usuário logado. Trocar de conta
 * cria uma sessão nova, então nada de uma conta vaza para a seguinte.
 */
class UserSession(
    val userId: String,
    private val database: AppDatabase
) {
    val syncManager = SyncManager(userId, database.driverDao(), database.vehicleDao())

    val vehicleRepository = VehicleRepository(userId, database.vehicleDao(), syncManager)
    val driverRepository = DriverRepository(userId, database.driverDao(), syncManager)
}

/**
 * Cria a sessão do [userId] e mantém a sincronização ligada enquanto a tela estiver viva,
 * refazendo o push sempre que a conexão voltar.
 */
@Composable
fun rememberUserSession(userId: String): UserSession {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember(context) { AppDatabase.getDatabase(context) }
    val session = remember(userId) { UserSession(userId, database) }

    DisposableEffect(session) {
        session.syncManager.startListening()
        scope.launch { session.syncManager.syncAll() }

        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                scope.launch { session.syncManager.syncAll() }
            }
        }
        connectivityManager?.registerDefaultNetworkCallback(networkCallback)

        onDispose {
            connectivityManager?.unregisterNetworkCallback(networkCallback)
            session.syncManager.close()
        }
    }

    return session
}

/** Apaga a cópia local ao sair da conta — o que está no Firestore permanece. */
suspend fun clearLocalData(context: Context) {
    val database = AppDatabase.getDatabase(context)
    database.vehicleDao().clearAll()
    database.driverDao().clearAll()
}
