package com.slemenceu.blehumidityapp.presentation.start_screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.slemenceu.blehumidityapp.data.utils.PermissionManager
import com.slemenceu.blehumidityapp.presentation.scan_screen.ScanScreenViewModel

@Composable
fun StartScreen(
    onNavigate: () -> Unit,
    viewModel: ScanScreenViewModel
) {
    val foundDevices = viewModel.foundDevices.collectAsState().value
    LaunchedEffect(foundDevices) {
        if (foundDevices.isNotEmpty()){
            onNavigate()
        }
    }
    val permissions = PermissionManager.permissionsArray
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissionsMap ->
            val areGranted = permissionsMap.values.reduce { acc, next -> acc && next }
            if (areGranted) {

            }
        }
    )
    LaunchedEffect(Unit) {
        if (!PermissionManager.checkAllPermissionsGranted(context = context)) {
            launcher.launch(permissions)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .background(
                    Color.Blue,
                    shape = CircleShape
                )
                .clickable(true){
                    viewModel.startScanning()
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "Start", fontSize = 30.sp,color = Color.White)
        }
    }
}

@Preview
@Composable
private fun StartScreenPreview() {
//    StartScreen(
//        onNavigate = {},
//
//    )
}