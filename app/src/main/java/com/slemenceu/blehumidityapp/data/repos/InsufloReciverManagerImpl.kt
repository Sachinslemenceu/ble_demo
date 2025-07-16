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
import android.os.Build
import android.util.Log
import androidx.compose.runtime.MutableState
import com.slemenceu.blehumidityapp.data.models.BleUnit
import com.slemenceu.blehumidityapp.data.models.BondState
import com.slemenceu.blehumidityapp.data.models.ConnectionState
import com.slemenceu.blehumidityapp.data.models.TempHumidityResult
import com.slemenceu.blehumidityapp.data.models.Resource
import com.slemenceu.blehumidityapp.data.utils.printGattTable
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
    private val DEVICE_NAME = "Insuflo"
    private val _devices = MutableSharedFlow<List<BluetoothDevice>>(replay = 1)
    override val devices: SharedFlow<List<BluetoothDevice>> = _devices.asSharedFlow()

    private val _connectionState =
        MutableStateFlow<ConnectionState>(ConnectionState.Uninitialized)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _bondState =
        MutableStateFlow<BondState>(BondState.Unchecked)
    override val bondState: StateFlow<BondState> = _bondState.asStateFlow()

    private val readQueue: MutableList<BluetoothGattCharacteristic> = mutableListOf()
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager


    private var _bleUnits: MutableList<BleUnit> = mutableListOf()


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
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Gatt status is sucess")
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, "Connected to gatt")
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
            } else {
                Log.d(TAG, "Failed to connect to gatt")
                gatt.disconnect()
                gatt.close()
                this@InsufloReceiverManagerImpl.gatt = null
                currentConnectAttempt += 1

                if (currentConnectAttempt <= MAX_CONNECTION_ATTEMPT) {
                    startReceiving(connectedDevice!!)
                } else {
                    coroutineScope.launch {
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                gatt.printGattTable()
                isAllServiceDiscovered = true
                coroutineScope.launch {
                    _connectionState.value = ConnectionState.Connected
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            this@InsufloReceiverManagerImpl.gatt?.services?.forEach {service->
                service.characteristics.forEach {char->
                    readQueue.addAll(service.characteristics)
                }
            }
            Log.d(TAG,"Status of on characteristic write $status ${readQueue.size}")
            readNextCharacteristic(this@InsufloReceiverManagerImpl.gatt!!)

        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            Log.d(TAG,"Status $status value ${value.toFloatList()}")
            readNextCharacteristic(this@InsufloReceiverManagerImpl.gatt!!)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            Log.d(TAG,"char ${characteristic.uuid} value ${value.toFloatList()}")
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
        Log.d(TAG, "The device recived is $device")
        connectedDevice = device
        _connectionState.value = ConnectionState.CurrentlyInitializing
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        device.connectGatt(context, false, gattCallback)
    }

    override fun sendOtp(otp: String) {
        val otpInt = otp.toInt()
        val byteVal: ByteArray = ByteBuffer
            .allocate(2)
            .order(ByteOrder.LITTLE_ENDIAN)  // 👈 Important!
            .putShort(otpInt.toShort())
            .array()
        Log.d(TAG, "Otp recived $otpInt")
        val char = gatt?.getService(suuid)?.getCharacteristic(cuuid)
        Log.d(TAG, "The char recived $char")
        if (isAllServiceDiscovered) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt?.writeCharacteristic(
                    char!!,
                    byteVal,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
            } else {
                char?.value = byteVal
                gatt?.writeCharacteristic(char)
            }
        } else{
            Log.d(TAG,"Service not discovered")
        }
    }

    override fun disconnect() {
        TODO("Not yet implemented")
    }

    override fun reconnect() {
        TODO("Not yet implemented")
    }

    override fun closeConnection() {
        gatt?.disconnect()
        gatt?.close()
    }

    private fun stopScanning() {
        bluetoothLeScanner.stopScan(bleScanCallback)
    }

    private fun BluetoothGattCharacteristic.isOnlyWrite(): Boolean {
        return properties == BluetoothGattCharacteristic.PROPERTY_WRITE
    }

    private val suuid = UUID.fromString("08233dea-d560-11ee-a506-0242ac120002")
    private val cuuid = UUID.fromString("08234060-d560-11ee-a506-0242ac120002")

    private fun readNextCharacteristic(gatt: BluetoothGatt) {
        if (readQueue.isNotEmpty()) {
            val nextChar = readQueue.removeAt(0)
            gatt.readCharacteristic(nextChar)
        }else {
            enableNotifications(this@InsufloReceiverManagerImpl.gatt!!)
        }
    }

    private fun ByteArray.toFloatList(): List<Float> {
        val buffer = ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN)
        val floatList = mutableListOf<Float>()
        while (buffer.remaining() >= 4) {
            floatList.add(buffer.float)
        }
        return floatList
    }

    private fun enableNotifications(gatt: BluetoothGatt) {
        Log.d(TAG, "Enabling notifications")
        val service = gatt.getService(UUID.fromString("08233dea-d560-11ee-a506-0242ac120002"))
        val notifyChar = service?.getCharacteristic(UUID.fromString("08234178-d560-11ee-a506-0242ac120002"))

        if (notifyChar == null) {
            Log.e(TAG, "Notification characteristic not found")
            return
        }

        if (notifyChar.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY == 0) {
            Log.e(TAG, "Characteristic does not support NOTIFY")
            return
        }

        Log.d(TAG, "Enabling GATT notification for ${notifyChar.uuid}")
        val result = gatt.setCharacteristicNotification(notifyChar, true)
        Log.d(TAG, "setCharacteristicNotification returned: $result")

        val descriptor = notifyChar.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
        if (descriptor != null) {
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            val writeResult = gatt.writeDescriptor(descriptor)
            Log.d(TAG, "Descriptor write initiated: $writeResult")
        } else {
            Log.e(TAG, "Descriptor is null, cannot enable notifications")
        }
    }


    override fun registerBondReceiver( targetDevice: BluetoothDevice) {
        val filter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val device = intent?.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
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