package com.slemenceu.blehumidityapp.domain

import android.bluetooth.BluetoothDevice
import com.slemenceu.blehumidityapp.data.models.BondState
import com.slemenceu.blehumidityapp.data.models.ConnectionState
import com.slemenceu.blehumidityapp.data.models.TempHumidityResult
import com.slemenceu.blehumidityapp.data.models.Resource
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface InsufloReceiverManager {


    val devices: SharedFlow<List<BluetoothDevice>>

    val connectionState: StateFlow<ConnectionState>

    val bondState: StateFlow<BondState>

    fun startScanning()

    fun startReceiving(device: BluetoothDevice)

    fun sendOtp(otp: String)

    fun registerBondReceiver(device: BluetoothDevice)

    fun disconnect()

    fun reconnect()

    fun closeConnection()
}