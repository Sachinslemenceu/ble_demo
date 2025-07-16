package com.slemenceu.blehumidityapp.data.models

import android.health.connect.datatypes.units.Temperature

data class TempHumidityResult(
    val temperature: Float,
    val humidity: Float,
    val connectionState: ConnectionState
)