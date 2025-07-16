package com.slemenceu.blehumidityapp.presentation.scan_screen

import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slemenceu.blehumidityapp.data.models.BondState
import com.slemenceu.blehumidityapp.data.models.ConnectionState
import com.slemenceu.blehumidityapp.data.models.Resource
import com.slemenceu.blehumidityapp.domain.InsufloReceiverManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("MissingPermission")
@HiltViewModel
class ScanScreenViewModel @Inject constructor(
    private val insufloReceiverManager: InsufloReceiverManager
) : ViewModel() {
    private val TAG = "ScanScreenViewModelLogs"
    private val _foundDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val foundDevices: StateFlow<List<BluetoothDevice>> = _foundDevices.asStateFlow()

    val connectionState = insufloReceiverManager.connectionState

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()


    fun startObserving() {
        viewModelScope.launch {
            insufloReceiverManager.devices.collect { devices ->
                _foundDevices.value = devices
                Log.d(TAG, "found devices ${_foundDevices.value}")
            }
        }
    }

    fun startScanning() {
        startObserving()
        insufloReceiverManager.startScanning()
    }

    fun startPairing(device: BluetoothDevice) {
        insufloReceiverManager.registerBondReceiver(device)
        when (device.bondState) {
            BluetoothDevice.BOND_BONDED -> {
                Log.d(TAG, "Already bonded, no need to bond again")
                _isLoading.value = false
                viewModelScope.launch {
                    delay(1000)
                    insufloReceiverManager.startReceiving(device)
                }
            }
            BluetoothDevice.BOND_BONDING -> {
                Log.d(TAG, "Currently bonding")
                _isLoading.value = true
            }
            BluetoothDevice.BOND_NONE -> {
                Log.d(TAG, "Not bonded, starting bonding process")
                device.createBond()
                _isLoading.value = true
            }
        }
        viewModelScope.launch {
            insufloReceiverManager.bondState.collect {
                when (it) {
                    BondState.Bonded -> {
                        Log.d(TAG, "The device is bonded")
                        _isLoading.value = false
                        viewModelScope.launch {
                            delay(3000)
                            insufloReceiverManager.startReceiving(device)
                        }
                    }
                    BondState.Bonding -> {
                        Log.d(TAG, "The device is bonding")
                        _isLoading.value = true
                    }
                    BondState.None -> {
                        Log.d(TAG, "The device is not bonded")
                        device.createBond()
                    }
                    BondState.Unchecked -> {}
                }
            }
        }
    }

    fun sendOtp(otp: String) {
        insufloReceiverManager.sendOtp(otp)
    }
}