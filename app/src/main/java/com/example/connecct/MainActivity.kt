package com.example.connecct

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.connecct.navigation.NavGraph
import com.example.connecct.ui.theme.ConnectTheme
import com.example.connecct.ui.viewmodel.ConnectionViewModel
import com.example.connecct.viewmodel.ThemeViewModel

class MainActivity : ComponentActivity() {

    // ViewModel untuk tema (gelap/terang)
    private val themeViewModel: ThemeViewModel by viewModels()

    // ViewModel untuk koneksi SSH
    private val connectionViewModel: ConnectionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ConnectTheme(darkTheme = themeViewModel.isDarkMode) {
                // Navigasi utama aplikasi
                NavGraph(
                    themeViewModel = themeViewModel,
                    connectionViewModel = connectionViewModel // ✅ diteruskan ke layar Connect
                )
            }
        }
    }
}
