package com.example.connecct.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class ThemeViewModel : ViewModel() {
    var isDarkMode by mutableStateOf(true)

    fun toggleTheme() {
        isDarkMode = !isDarkMode
    }
}