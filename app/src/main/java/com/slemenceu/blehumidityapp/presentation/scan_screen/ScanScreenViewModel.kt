package com.slemenceu.blehumidityapp.presentation.scan_screen

import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slemenceu.blehumidityapp.data.models.BondState
import com.slemenceu.blehumidityapp.data.models.ConnectionState
import com.slemenceu.blehumidityapp.data.models.OTPState
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

    val data = insufloReceiverManager.data

    val connectionState = insufloReceiverManager.connectionState
    val _pairText = MutableStateFlow("")
    val pairText = _pairText.asStateFlow()
    val _connectText = MutableStateFlow("")
    val connectText = _connectText.asStateFlow()
    val otpState = insufloReceiverManager.otpState

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var isStartConnecting = false

    private var isBondingInProgress = false
    init {
        startObserving()
    }

    fun startObserving() {
        viewModelScope.launch {
            insufloReceiverManager.devices.collect { devices ->
                _foundDevices.value = devices
                Log.d(TAG, "found devices ${_foundDevices.value}")
            }

        }
        viewModelScope.launch {

            insufloReceiverManager.connectionState.collect {
                when (it) {
                    ConnectionState.Connected -> {
                        Log.d(TAG, "Connected")
                        _connectText.value = "Disconnect"
                        isStartConnecting = false
                    }
                    ConnectionState.ConnectionAttemptFailed -> {
                        Log.d(TAG, "Connection attempt failed")
                    }
                    ConnectionState.Disconnected -> {
                        Log.d(TAG, "Disconnected")
                        _connectText.value = "Connect"
                    }
                    ConnectionState.Uninitialized -> {
                        Log.d(TAG, "Uninitialized")
                    }
                }
            }
        }
        viewModelScope.launch {
            insufloReceiverManager.otpState.collect {
                when(it){
                    OTPState.OTPNotVerified -> {
                        Log.d(TAG, "OTP not verified")
                    }
                    OTPState.OTPVerificationFailed -> {
                        Log.d(TAG, "OTP verification failed")
                        _isLoading.value = false
                    }
                    OTPState.OTPVerificationSuccessfull -> {
                        _isLoading.value = false
                        Log.d(TAG, "OTP verification successfull")
                    }
                }
            }
        }
    }

    fun startScanning() {
        insufloReceiverManager.startScanning()
    }

    fun startPairing(device: BluetoothDevice) {
        insufloReceiverManager.registerBondReceiver(device)
        if (device.bondState != BluetoothDevice.BOND_BONDED) {
            device.createBond()
        }
        when (device.bondState) {
            BluetoothDevice.BOND_BONDED -> {
                Log.d(TAG, "Already bonded, no need to bond again")
                _isLoading.value = false
                _pairText.value = "Unpair"
                viewModelScope.launch {
                    delay(1000)
                    if (!isStartConnecting) {
                        isStartConnecting = true
                        insufloReceiverManager.startReceiving(device)
                    }
                }
            }
            BluetoothDevice.BOND_BONDING -> {
                Log.d(TAG, "Currently bonding")
                _isLoading.value = true
            }
            BluetoothDevice.BOND_NONE -> {
                Log.d(TAG, "Not bonded, starting bonding process")
//                device.createBond()
                _pairText.value = "Pair"
                _isLoading.value = true
            }
        }
        viewModelScope.launch {
            insufloReceiverManager.bondState.collect {
                when (it) {
                    BondState.Bonded -> {
                        Log.d(TAG, "The device is bonded")
                        _isLoading.value = false
                        isBondingInProgress = false
                        viewModelScope.launch {
                            delay(1000)
                            if (!isStartConnecting){
                                insufloReceiverManager.startReceiving(device)
                                isStartConnecting = true
                            }
                        }
                    }

                    BondState.Bonding -> {
                        Log.d(TAG, "The device is bonding")
                        _isLoading.value = true
                    }

                    BondState.None -> {
                        Log.d(TAG, "The device is not bonded")
                        if (!isBondingInProgress){
                            device.createBond()
                            isBondingInProgress = true
                        }
                    }

                    BondState.Unchecked -> {}
                }
            }
        }
    }

    fun sendOtp(otp: String) {

        if (insufloReceiverManager.connectionState.value == ConnectionState.Connected) {
            insufloReceiverManager.sendOtp(otp)
            viewModelScope.launch {
                _isLoading.value = true
            }
        } else{
            Log.d(TAG, "The device is not connected")

        }
    }

    fun disconnect(){
        insufloReceiverManager.disconnect()
    }

    fun unpair(){
        insufloReceiverManager.unpair()
    }
}