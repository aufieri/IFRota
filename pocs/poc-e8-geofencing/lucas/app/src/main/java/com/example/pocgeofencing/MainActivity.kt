package com.example.pocgeofencing

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
import com.example.pocgeofencing.ui.GeofenceScreen
import com.example.pocgeofencing.ui.GeofenceViewModel
import com.example.pocgeofencing.ui.theme.PocGeofencingTheme
import org.maplibre.android.MapLibre

class MainActivity : ComponentActivity() {

    private val geofenceViewModel: GeofenceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Precisa ser inicializado antes de criar qualquer MapView.
        MapLibre.getInstance(applicationContext)

        enableEdgeToEdge()
        setContent {
            PocGeofencingTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GeofenceScreen(viewModel = geofenceViewModel)
                }
            }
        }
    }
}
