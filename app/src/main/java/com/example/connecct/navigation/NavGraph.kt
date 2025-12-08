package com.example.connecct.navigation

import com.example.connecct.ui.screen.DeviceScreen
import com.example.connecct.ui.viewmodel.DeviceViewModelFactory
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.connecct.components.BottomNavBar
import com.example.connecct.ui.screen.*
import com.example.connecct.ui.viewmodel.DeviceViewModel
import com.example.connecct.ui.viewmodel.KeysViewModel
import com.example.connecct.ui.viewmodel.ConnectionViewModel
import com.example.connecct.viewmodel.ThemeViewModel
import com.example.connecct.storage.PinStorage
import com.example.connecct.viewmodel.PinViewModel
import com.example.connecct.viewmodel.PinViewModelFactory


@Composable
fun NavGraph(
    themeViewModel: ThemeViewModel,
    connectionViewModel: ConnectionViewModel,
) {
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute != "pin") {
                BottomNavBar(navController)
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "pin",
            modifier = Modifier.padding(paddingValues)
        ) {
            // PIN
            composable("pin") {
                val context = LocalContext.current
                val pinViewModel: PinViewModel = viewModel(
                    factory = PinViewModelFactory(context.applicationContext)
                )

                PinScreen(
                    viewModel = pinViewModel,
                    onSuccess = {
                        navController.navigate("connect") {
                            popUpTo("pin") { inclusive = true }
                        }
                    }
                )
            }

            composable("connect") {
                val context = LocalContext.current
                val deviceViewModel: DeviceViewModel = viewModel(
                    factory = DeviceViewModelFactory(context.applicationContext)
                )
                ConnectScreen(
                    viewModel = connectionViewModel,
                    navController = navController,
                    deviceViewModel = deviceViewModel
                )
            }

            composable("connect_beta") {
                ConnectBetaScreen(
                    viewModel = connectionViewModel,
                    navController = navController
                )
            }

            composable("devices") {
                val context = LocalContext.current
                val deviceViewModel: DeviceViewModel = viewModel(
                    factory = DeviceViewModelFactory(context.applicationContext)
                )

                DeviceScreen(viewModel = deviceViewModel)
            }

            composable("history") {
                HistoryScreen()
            }

            composable("settings") {
                val keyViewModel: KeysViewModel = viewModel()
                SettingsScreen(
                    themeViewModel = themeViewModel,
                    keyViewModel = keyViewModel,
                    connectionViewModel = connectionViewModel,
                    navController = navController
                )
            }
        }
    }
}