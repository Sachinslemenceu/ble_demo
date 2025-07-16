package com.slemenceu.blehumidityapp.presentation.scan_screen.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScanDevice(
    deviceName: String,
    deviceAddress: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Gray
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
            ) {
                Text(
                    text = deviceName,
                    fontSize = 20.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = deviceAddress,
                    fontSize = 15.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Light
                )

            }
            IconButton(
                onClick = { onClick() },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.LightGray
                )
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Connect",
                    tint = Color.White
                )
            }
        }
    }
}

@Preview
@Composable
private fun ScanDevicePreview() {
    ScanDevice(
        deviceName = "Insuflo",
        deviceAddress = "123456789",
        onClick = {}
    )
}