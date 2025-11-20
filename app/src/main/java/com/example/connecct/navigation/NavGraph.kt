package com.example.connecct.navigation

import DeviceScreen
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.connecct.components.BottomNavBar
import com.example.connecct.ui.screen.ConnectScreen
import com.example.connecct.ui.screen.ConnectBetaScreen
import com.example.connecct.ui.screen.HistoryScreen
import com.example.connecct.ui.screen.SettingsScreen
import com.example.connecct.ui.viewmodel.DeviceViewModel
import com.example.connecct.ui.viewmodel.KeysViewModel
import com.example.connecct.ui.viewmodel.ConnectionViewModel
import com.example.connecct.viewmodel.ThemeViewModel

@Composable
fun NavGraph(
    themeViewModel: ThemeViewModel,
    connectionViewModel: ConnectionViewModel
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavBar(navController) }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "connect",
            modifier = Modifier.padding(paddingValues)
        ) {
            // CONNECT SCREEN
            composable("connect") {
                ConnectScreen(
                    viewModel = connectionViewModel,
                    navController = navController
                )
            }

            // CONNECT BETA SCREEN  ← BARU
            composable("connect_beta") {
                ConnectBetaScreen(
                    viewModel = connectionViewModel,
                    navController = navController
                )
            }

            // DEVICES SCREEN (pengganti keys)
            composable("devices") {
                val deviceViewModel: DeviceViewModel = viewModel()
                DeviceScreen(viewModel = deviceViewModel)
            }

            // HISTORY SCREEN
            composable("history") {
                HistoryScreen()
            }

            // SETTINGS SCREEN — kirimkan KeysViewModel seperti semula
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
