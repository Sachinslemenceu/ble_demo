package com.slemenceu.blehumidityapp.presentation.scan_screen

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.slemenceu.blehumidityapp.data.models.ConnectionState
import com.slemenceu.blehumidityapp.presentation.scan_screen.composables.ScanDevice
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("MissingPermission")
@Composable
fun ScanScreen(
    viewModel: ScanScreenViewModel
) {
    var showDialog  by remember{ mutableStateOf(false) }
    var otp by remember { mutableStateOf("") }
    val foundDevices = viewModel.foundDevices.collectAsState()

    val connectionState = viewModel.connectionState.collectAsState()
    LaunchedEffect(connectionState.value) {
        if (connectionState.value is ConnectionState.Connected) {
            showDialog = true
        }
    }
    Log.d("ScanScreenLogs","found devices ${foundDevices.value}")
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Button(
                        onClick = {
                            viewModel.startScanning()
                        }
                    ) {
                        Text("Scan Device")
                    }
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Scanned Devices")
            LazyColumn {
                items(foundDevices.value.size) {
                    ScanDevice(
                        deviceName = foundDevices.value[it].name ?: "No Name",
                        deviceAddress = foundDevices.value[it].address ?: "No Address",
                        onClick = {
                            viewModel.startPairing(foundDevices.value[it])
                        }
                    )
                }
            }

        }
    }
    if (showDialog) {
        Dialog(
            onDismissRequest = {
                showDialog = false
            }
        ) {
            Column {
                TextField(
                    value = otp,
                    onValueChange = {
                        otp = it
                    }
                )
                Button(
                    onClick = {
                        viewModel.sendOtp(otp)
                    }
                ) {
                    Text(text = "Send OTP")
                }
            }
        }
    }
    if (viewModel.isLoading.collectAsState().value){
        CircularProgressIndicator()
    }
}

@Preview
@Composable
private fun ScanScreenPreview() {
//    ScanScreen()
}