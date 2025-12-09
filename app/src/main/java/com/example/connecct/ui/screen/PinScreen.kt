package com.example.connecct.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.connecct.MainActivity
import com.example.connecct.viewmodel.PinViewModel

@Composable
fun PinScreen(
    viewModel: PinViewModel,
    onSuccess: () -> Unit
) {
    if (viewModel.authenticated.value) {
        onSuccess()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = if (viewModel.pinExists.value) "Masukkan PIN" else "Buat PIN Baru",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(16.dp))

        Row(Modifier.padding(20.dp)) {
            repeat(4) { index ->
                val filled = viewModel.pinInput.value.length > index
                Box(
                    Modifier
                        .size(16.dp)
                        .padding(6.dp)
                        .background(
                            if (filled) Color.Black else Color.Gray,
                            CircleShape
                        )
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9")
        )

        rows.forEach { row ->
            Row {
                row.forEach { digit ->
                    Button(
                        onClick = {
                            viewModel.addDigit(digit)
                            if (viewModel.pinInput.value.length == 4)
                                viewModel.submitPin()
                        },
                        modifier = Modifier
                            .padding(6.dp)
                            .size(80.dp)
                    ) {
                        Text(digit)
                    }
                }
            }
        }

        Row {
            Spacer(Modifier.size(80.dp))
            Button(
                onClick = {
                    viewModel.addDigit("0")
                    if (viewModel.pinInput.value.length == 4)
                        viewModel.submitPin()
                },
                modifier = Modifier
                    .padding(6.dp)
                    .size(80.dp)
            ) {
                Text("0")
            }
            Spacer(Modifier.size(80.dp))
        }

        Spacer(Modifier.height(10.dp))

        Button(onClick = { viewModel.deleteDigit() }) {
            Text("Hapus")
        }

        if (viewModel.pinExists.value) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { viewModel.resetPin() }) {
                Text("Lupa PIN? Hapus & buat baru")
            }
        }
    }
}
