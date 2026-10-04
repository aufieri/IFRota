package com.example.pocmapa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.pocmapa.ui.screens.FleetMapScreen
import com.example.pocmapa.ui.screens.FleetViewModel
import com.example.pocmapa.ui.theme.PocMapaTheme
import org.maplibre.android.MapLibre

class MainActivity : ComponentActivity() {

    private val fleetViewModel: FleetViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Precisa ser inicializado antes de criar qualquer MapView.
        MapLibre.getInstance(applicationContext)

        enableEdgeToEdge()
        setContent {
            PocMapaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FleetMapScreen(viewModel = fleetViewModel)
                }
            }
        }
    }
}
