package com.slemenceu.blehumidityapp.data.models

import java.util.UUID

data class BleUnit(
    val serviceUUID: UUID,
    val characteristicUUID: UUID,
)
