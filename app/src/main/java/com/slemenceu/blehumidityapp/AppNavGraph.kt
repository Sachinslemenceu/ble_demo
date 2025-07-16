package com.slemenceu.blehumidityapp

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.slemenceu.blehumidityapp.presentation.humidity_temp_screen.HumidityTempScreen
import com.slemenceu.blehumidityapp.presentation.scan_screen.ScanScreen
import com.slemenceu.blehumidityapp.presentation.scan_screen.ScanScreenViewModel
import com.slemenceu.blehumidityapp.presentation.start_screen.StartScreen
import kotlinx.serialization.Serializable

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val viewModel: ScanScreenViewModel = hiltViewModel()

    NavHost(
        navController = navController,
        startDestination = StartScreen
    ){
        composable<StartScreen>{
            StartScreen(
                onNavigate = {
                    navController.navigate(ScanScreen)
                },
                viewModel = viewModel
            )
        }
        composable<HumidityTempScreen>{
            HumidityTempScreen()
        }
        composable<ScanScreen>{
            ScanScreen(
                viewModel = viewModel
            )
        }
    }
}

@Serializable
object StartScreen

@Serializable
object HumidityTempScreen

@Serializable
object ScanScreen