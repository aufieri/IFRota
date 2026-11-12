package br.edu.ifsp.ifrota.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.edu.ifsp.ifrota.core.clearLocalData
import br.edu.ifsp.ifrota.ui.components.PrimaryButton
import br.edu.ifsp.ifrota.ui.theme.Surface2
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun AdminScaffold(userId: String, accountEmail: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface2)
            .statusBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Área Administrativa",
            style = MaterialTheme.typography.headlineMedium,
            color = Color(0xFF2E7D32)
        )
        Text(
            text = accountEmail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        PrimaryButton(
            text = "Sair",
            onClick = {
                scope.launch {
                    clearLocalData(context)
                    FirebaseAuth.getInstance().signOut()
                }
            }
        )
    }
}
