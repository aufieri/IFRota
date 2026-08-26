package com.example.pocsqlite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.pocsqlite.data.local.AppDatabase
import com.example.pocsqlite.data.repository.DriverRepository
import com.example.pocsqlite.data.repository.VehicleRepository
import com.example.pocsqlite.ui.screens.DriverRegistrationScreen
import com.example.pocsqlite.ui.screens.VehicleRegistrationScreen
import com.example.pocsqlite.ui.theme.PocSqliteTheme
import com.example.pocsqlite.ui.viewmodel.DriverRegistrationViewModel
import com.example.pocsqlite.ui.viewmodel.DriverRegistrationViewModelFactory
import com.example.pocsqlite.ui.viewmodel.VehicleRegistrationViewModel
import com.example.pocsqlite.ui.viewmodel.VehicleRegistrationViewModelFactory

class MainActivity : ComponentActivity() {

    private val vehicleRepository by lazy {
        VehicleRepository(AppDatabase.getDatabase(applicationContext).vehicleDao())
    }
    private val driverRepository by lazy {
        DriverRepository(AppDatabase.getDatabase(applicationContext).driverDao())
    }

    private val vehicleViewModel: VehicleRegistrationViewModel by viewModels {
        VehicleRegistrationViewModelFactory(vehicleRepository)
    }
    private val driverViewModel: DriverRegistrationViewModel by viewModels {
        DriverRegistrationViewModelFactory(driverRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PocSqliteTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var selectedTab by remember { mutableIntStateOf(0) }

                    Column {
                        SecondaryTabRow(selectedTabIndex = selectedTab) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { Text("Veículos") }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text("Motoristas") }
                            )
                        }

                        if (selectedTab == 0) {
                            VehicleRegistrationScreen(viewModel = vehicleViewModel)
                        } else {
                            DriverRegistrationScreen(viewModel = driverViewModel)
                        }
                    }
                }
            }
        }
    }
}
