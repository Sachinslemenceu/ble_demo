package com.slemenceu.blehumidityapp.presentation.main_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slemenceu.blehumidityapp.data.models.ConnectionState
import com.slemenceu.blehumidityapp.presentation.scan_screen.ScanScreen
import com.slemenceu.blehumidityapp.presentation.scan_screen.ScanScreenViewModel

@Composable
fun MainScreen(
    viewModel: ScanScreenViewModel
) {

    var items by remember { mutableStateOf(listOf<Float>()) }
    val data = viewModel.data.collectAsState().value

    val pairText = viewModel.pairText.collectAsState().value
    val connectText = viewModel.connectText.collectAsState().value
    LaunchedEffect(data) {
        items = items + data
    }
    LaunchedEffect(Unit) {
    }
    Scaffold {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(15.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(
                        onClick = {},
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Blue,
                            contentColor = Color.White
                        )
                    ) {
                        Text(connectText)
                    }
                    OutlinedButton(
                        onClick = {

                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Green,
                            contentColor = Color.White
                        )
                    ) {
                        Text(pairText)
                    }
                }
            }
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(10.dp)
            ) {
                items(items) { item ->
                    Text(
                        text = item.toString(),
                        fontSize = 11.sp,
                        overflow = TextOverflow.Clip,
                    )
                }
            }
        }
    }
}


@Preview
@Composable
private fun MainScreenPreview() {
//    MainScreen()
}