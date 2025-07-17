package com.slemenceu.blehumidityapp.data.repos

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.aware.Characteristics
import android.os.Build
import android.util.Log
import androidx.compose.runtime.MutableState
import com.slemenceu.blehumidityapp.data.models.BleUnit
import com.slemenceu.blehumidityapp.data.models.BondState
import com.slemenceu.blehumidityapp.data.models.ConnectionState
import com.slemenceu.blehumidityapp.data.models.OTPState
import com.slemenceu.blehumidityapp.data.models.TempHumidityResult
import com.slemenceu.blehumidityapp.data.models.Resource
import com.slemenceu.blehumidityapp.data.utils.characteristicMap
import com.slemenceu.blehumidityapp.data.utils.logCharacteristicValues
import com.slemenceu.blehumidityapp.data.utils.printGattTable
import com.slemenceu.blehumidityapp.data.utils.startPeriodicReading
import com.slemenceu.blehumidityapp.data.utils.toByteArray
import com.slemenceu.blehumidityapp.data.utils.toFloatList
import com.slemenceu.blehumidityapp.domain.InsufloReceiverManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import javax.inject.Inject

@Suppress("MissingPermission")
class InsufloReceiverManagerImpl @Inject constructor(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter
) : InsufloReceiverManager {
    private val TAG = "BLELOgs"

    private val SUUID_OTP = UUID.fromString("08233dea-d560-11ee-a506-0242ac120002")
    private val CUUID_OTP = UUID.fromString("08234060-d560-11ee-a506-0242ac120002")

    private val _devices = MutableSharedFlow<List<BluetoothDevice>>(replay = 1)
    override val devices: SharedFlow<List<BluetoothDevice>> = _devices.asSharedFlow()

    private val _connectionState =
        MutableStateFlow<ConnectionState>(ConnectionState.Uninitialized)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _otpState =
        MutableStateFlow<OTPState>(OTPState.OTPNotVerified)

    override val otpState: StateFlow<OTPState> = _otpState.asStateFlow()

    private val _bondState =
        MutableStateFlow<BondState>(BondState.Unchecked)
    override val bondState: StateFlow<BondState> = _bondState.asStateFlow()

    private val _data = MutableStateFlow<List<Float>>(emptyList())
    override val data: StateFlow<List<Float>> = _data.asStateFlow()

    //    private val readQueue: MutableList<BluetoothGattCharacteristic> = mutableListOf()
    private val characteristicsList: MutableList<BluetoothGattCharacteristic> = mutableListOf()

    private var currentIndex = 0

    private var gatt: BluetoothGatt? = null

    private val bluetoothLeScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private var connectedDevice: BluetoothDevice? = null

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
        .setReportDelay(5000)
        .build()

    private var isScanning = false

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val foundDevice: MutableList<BluetoothDevice> = mutableListOf()

    private var currentConnectAttempt = 1

    private var isAllServiceDiscovered = false
    private val MAX_CONNECTION_ATTEMPT = 5

    private var isReading = true

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Gatt status is sucess")
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, "Connected to gatt")
                    coroutineScope.launch {
                        _connectionState.value = ConnectionState.Connected
                    }
                    gatt.discoverServices()
                    this@InsufloReceiverManagerImpl.gatt = gatt
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(TAG, "Disconnected from gatt")
                    coroutineScope.launch {
                        _connectionState.value = ConnectionState.Disconnected
                    }
                    gatt.close()
                    this@InsufloReceiverManagerImpl.gatt = null
                }
            } else if (status == BluetoothGatt.GATT_FAILURE) {
                Log.d(TAG, "Failed to connect to gatt")
                gatt.disconnect()
                gatt.close()
                this@InsufloReceiverManagerImpl.gatt = null
                currentConnectAttempt += 1

                if (currentConnectAttempt <= MAX_CONNECTION_ATTEMPT) {
                    startReceiving(connectedDevice!!)
                } else {
                    coroutineScope.launch {
                        _connectionState.value = ConnectionState.ConnectionAttemptFailed
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                gatt.printGattTable()
                isAllServiceDiscovered = true

                gatt.services.forEach { service ->
                    service.characteristics.forEach { characteristic ->
                        val charUuid = characteristic.uuid.toString().lowercase()
                        if (characteristicMap.containsKey(charUuid) &&
                            characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0
                        ) {
                            characteristicsList.add(characteristic)
                        }
                    }
                }

            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {

            Log.d(TAG, "Status of on characteristic write $status ${characteristicsList.size}")
            readNextCharacteristic(this@InsufloReceiverManagerImpl.gatt!!)


        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION) {
                _otpState.value = OTPState.OTPVerificationFailed
                return
            }
            _otpState.value = OTPState.OTPVerificationSuccessfull
//            Log.d(TAG, "Status $status value ${value.toFloatList()}")
            _data.value = value.toFloatList()
            logCharacteristicValues(value, characteristic.uuid.toString())
            readNextCharacteristic(this@InsufloReceiverManagerImpl.gatt!!)
        }


        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Descriptor written successfully")
            } else if (status == BluetoothGatt.GATT_FAILURE) {
                Log.d(TAG, "Descriptor write failed")
            }

        }
    }

    private val bleScanCallback = object : ScanCallback() {
        override fun onBatchScanResults(results: List<ScanResult?>?) {
            Log.d(TAG, "Scan results $results")
            if (results != null) {
                for (result in results) {
                    val device = result?.device
                    if (device != null && device.name != null) {
                        val alreadyExists = foundDevice.any { it.address == device.address }
                        if (!alreadyExists) {
                            foundDevice.add(device)
                        }
                    }
                }
                coroutineScope.launch {
                    _devices.emit(foundDevice)
                    Log.d(TAG, "emitted devices $foundDevice")
                    isScanning = false
                    stopScanning()
                }
            } else {
                coroutineScope.launch {
                    isScanning = false
                    stopScanning()
                }
            }
        }
    }

    override fun startScanning() {
        if (isScanning) return
        Log.d(TAG, "startScanning: ")
        isScanning = true
        bluetoothLeScanner.startScan(null, scanSettings, bleScanCallback)
    }

    override fun startReceiving(device: BluetoothDevice) {
        if (gatt == null) {
            Log.d(TAG, "The device recived is $device")
            connectedDevice = device
            gatt?.disconnect()
            gatt?.close()
            gatt = null
            device.connectGatt(context, false, gattCallback)
        } else{
            Log.d(TAG, "The device is already connected")
        }
    }

    override fun sendOtp(otp: String) {
        val otpInt = otp.toInt()
        val byteVal = otpInt.toByteArray()
        Log.d(TAG, "Otp recived $otpInt")
        val char = gatt?.getService(SUUID_OTP)?.getCharacteristic(CUUID_OTP)
        Log.d(TAG, "The char recived $char")
        if (isAllServiceDiscovered) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt?.writeCharacteristic(
                    char!!,
                    byteVal,
                    BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                )
            } else {
                char?.value = byteVal
                gatt?.writeCharacteristic(char)
            }
        } else {
            Log.d(TAG, "Service not discovered")
        }
    }

    override fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
    }

    override fun reconnect() {
        startReceiving(connectedDevice!!)
    }

    override fun unpair() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        try {
            val method = connectedDevice?.javaClass?.getMethod("removeBond")
            method?.invoke(connectedDevice)
            Log.d(TAG, "Unpair success")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(TAG, "Unpair failed")
        }
    }



    private fun stopScanning() {
        bluetoothLeScanner.stopScan(bleScanCallback)
    }

    private fun readNextCharacteristic(gatt: BluetoothGatt) {
        if (characteristicsList.isNotEmpty() && isReading) {
            val nextChar = characteristicsList[currentIndex]
            gatt.readCharacteristic(nextChar)
            Log.d(TAG, "Reading characteristic ${nextChar.uuid} at ${System.currentTimeMillis()}")
            currentIndex = (currentIndex + 1) % characteristicsList.size // Loop back to 0
        }
    }

     fun startReading() {
        isReading = false
    }

    override fun registerBondReceiver(targetDevice: BluetoothDevice) {
        val filter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val device =
                    intent?.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val bondState = intent?.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)

                // ✅ Filter for specific device by MAC address
                if (device?.address == targetDevice.address) {
                    when (bondState) {
                        BluetoothDevice.BOND_BONDED -> {
                            _bondState.value = BondState.Bonded
                            Log.d("BondReceiver", "Bonded with ${device.address}")
                        }

                        BluetoothDevice.BOND_NONE -> {
                            _bondState.value = BondState.None
                            Log.d("BondReceiver", "Bond removed for ${device.address}")
                        }

                        BluetoothDevice.BOND_BONDING -> {
                            _bondState.value = BondState.Bonding
                            Log.d("BondReceiver", "Bonding with ${device.address}")
                        }
                    }
                }
            }
        }, filter)

}

}