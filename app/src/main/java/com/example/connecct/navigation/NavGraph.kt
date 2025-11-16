package com.example.connecct.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.connecct.components.BottomNavBar
import com.example.connecct.ui.screen.*
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
            modifier = androidx.compose.ui.Modifier.padding(paddingValues)
        ) {
            // ✅ kirim ViewModel koneksi ke layar Connect
            composable("connect") { ConnectScreen(viewModel = connectionViewModel, navController = navController) }
            composable("keys") { KeysScreen() }
            composable("history") { HistoryScreen() }
            composable("settings") { SettingsScreen(themeViewModel) }
        }
    }
}
