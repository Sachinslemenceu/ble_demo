package com.slemenceu.blehumidityapp.data.utils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder

private val mainHandler = Handler(Looper.getMainLooper())
fun ByteArray.toFloatList(): List<Float> {
    val buffer = ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN)
    val floatList = mutableListOf<Float>()
    while (buffer.remaining() >= 4) {
        floatList.add(buffer.float)
    }
    return floatList
}

fun BluetoothGattCharacteristic.isOnlyWrite(): Boolean {
    return properties == BluetoothGattCharacteristic.PROPERTY_WRITE
}

fun logCharacteristicValues(value: ByteArray, uuid: String) {
    if (value.isEmpty()) {
        Log.w("BLELOgs", "Empty ByteArray received for UUID: $uuid")
        return
    }

    val buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN)
    val paramNames = characteristicMap[uuid]?.values?.toList() ?: emptyList()
    if (paramNames.isEmpty()) {
        Log.w("BLELOgs", "No mapping found for UUID: $uuid")
        return
    }

    Log.d("BLELOgs", "Processing characteristic $uuid with ${value.size} bytes: ${value.joinToString { it.toString(16).padStart(2, '0') }}")

    for (i in paramNames.indices) {
        if (buffer.remaining() >= 2) {
            val shortValue = buffer.short.toFloat() / 10f // Scale by 10 (e.g., 200 -> 20.0)
            Log.d("BLELOgs", "${paramNames[i]}: $shortValue")
        } else {
            Log.w("BLELOgs", "Insufficient data for ${paramNames[i]}, remaining bytes: ${buffer.remaining()}")
            break
        }
    }
}

fun Int.toByteArray(): ByteArray {
    val byteVal: ByteArray = ByteBuffer
        .allocate(2)
        .order(ByteOrder.LITTLE_ENDIAN)  // 👈 Important!
        .putShort(this.toShort())
        .array()
    return byteVal
}



fun stopPeriodicReading() {
    mainHandler.removeCallbacksAndMessages(null) // Cancel all pending callbacks
    Log.d("BLELOgs", "Periodic reading stopped")
}

val characteristicMap = mapOf(

    // define insulin pump parameters

    // characteristic to map ble data1
    "f95c620e-d479-11ee-a506-0242ac120002" to mapOf(
        0 to "Battery",
        1 to "Reservoir",
        2 to "QuickBolusSet",
        3 to "QuickBolusIncrement",
        4 to "InfusionSetChange",
        5 to "temperature",
        6 to "AlarmAudVib"
    ),

    // characteristic to map ble data2
    "f95c6326-d479-11ee-a506-0242ac120002" to mapOf(
        0 to "BasalPattern",
        1 to "BasalRate",
        2 to "BGTargetNum",
        3 to "BGTargetUL",
        4 to "BGTargetLL",
        5 to "CarbRatioNum",
        6 to "CarbRatioVal",
        7 to "ISFNum",
        8 to "ISFVal"
    ),

    // characteristic to map ble data3
    "5a627ad0-d546-11ee-a506-0242ac120002" to mapOf(
        0 to "BolusCalcStatus",
        1 to "BGFeature",
        2 to "MaxBasal",
        3 to "MaxBolus",
        4 to "ExtendedBolus",
        5 to "CombBolus",
        6 to "InsDel",
        7 to "IOBVal"
    ),

    // characteristic to map ble data4
    "5a627c1a-d546-11ee-a506-0242ac120002" to mapOf(
        0 to "BolusType",
        1 to "StartTime",
        2 to "BolusDelivered",
        3 to "TotalBolus"
    ),

    // characteristic to map BGCarb props
    "46f99c20-d560-11ee-a506-0242ac120002" to mapOf(
        0 to "UniqueBGCarbTime",
        1 to "UniqueBGCarbDate",
        2 to "BGVal",
        3 to "CarbVal",
        4 to "InitialCalcData",
        5 to "FinalCalcData"
    ),

    // characteristic to map ALARM props
    "46f99d56-d560-11ee-a506-0242ac120002" to mapOf(
        0 to "AlarmUniqueTime",
        1 to "AlarmUniqueDate",
        2 to "AlarmCode"
    ),

    // characteristic to map BOLUS props
    "52bed620-d56e-11ee-a506-0242ac120002" to mapOf(
        0 to "BolusUniqueTime",
        1 to "BolusUniqueDate",
        2 to "BolusType",
        3 to "BolusVal",
        4 to "BolusTime"
    )
)

val serviceMap = mapOf(
    // Service for general pump status
    "f95c620e-d479-11ee-a506-0242ac120002" to "F95C5FE8-D479-11EE-A506-0242AC120002", // Battery, Reservoir, etc.
    "f95c6326-d479-11ee-a506-0242ac120002" to "F95C5FE8-D479-11EE-A506-0242AC120002", // BasalPattern, BasalRate, etc.

    // Service for basal and bolus settings
    "5a627ad0-d546-11ee-a506-0242ac120002" to "5A627760-D546-11EE-A506-0242AC120002", // BolusCalcStatus, MaxBasal, etc.
    "5a627c1a-d546-11ee-a506-0242ac120002" to "5A627760-D546-11EE-A506-0242AC120002", // BolusType, StartTime, etc.

    // Service for BG, carb, and alarm data
    "46f99c20-d560-11ee-a506-0242ac120002" to "46F996E4-D560-11EE-A506-0242AC120002", // UniqueBGCarbTime, BGVal, etc.
    "46f99d56-d560-11ee-a506-0242ac120002" to "46F996E4-D560-11EE-A506-0242AC120002", // AlarmUniqueTime, AlarmCode

    // Service for bolus data
    "52bed620-d56e-11ee-a506-0242ac120002" to "52BED3BE-D56E-11EE-A506-0242AC120002"  // BolusUniqueTime, BolusVal, etc.
)


//private fun updateRepositoryWithCharacteristic(
//    characteristic: BluetoothGattCharacteristic,
//    value: ByteArray,
//    status: Int,
//    bleDataRepository: BleDataRepository
//) {
//    if (status != BluetoothGatt.GATT_SUCCESS) return // Exit if read failed
//
//    val uuid = characteristic.uuid.toString().lowercase()
//    val hex = value.joinToString(separator = "") { "%02X".format(it).uppercase() }
//    val substrings = mutableListOf<String>()
//
//    // Break hex string into 8-character chunks
//    for (i in hex.indices step 8) {
//        val substring = if (i + 8 <= hex.length) {
//            hex.substring(i, i + 8)
//        } else {
//            hex.substring(i)
//        }
//        substrings.add(substring)
//    }
//
//    // Convert hex to float values (assuming IEEE 754 format, similar to GattClient)
//    val convertedDataList = substrings.map { java.lang.Float.intBitsToFloat((it.substring(6) + it.substring(4, 6) + it.substring(2, 4) + it.substring(0, 2)).toLong(16).toInt()) }
//
//    // Create a map of parameter names and values
//    val updateMap: MutableMap<String, Float> = mutableMapOf()
//    characteristicMap[uuid]?.forEach { (index, paramName) ->
//        if (index < convertedDataList.size) {
//            updateMap[paramName] = convertedDataList[index]
//        }
//    }
//
//    // Update repository based on UUID
//    coroutineScope.launch(Dispatchers.IO) {
//        try {
//            when (uuid) {
//                "46f99c20-d560-11ee-a506-0242ac120002" -> { // BGCarb properties
//                    Log.d(TAG, "Updating BG properties: $updateMap")
//                    bleDataRepository.updateBgProperty(updateMap)
//                }
//                "46f99d56-d560-11ee-a506-0242ac120002" -> { // Alarm properties
//                    Log.d(TAG, "Updating Alarm properties: $updateMap")
//                    bleDataRepository.updateAlarmProperty(updateMap)
//                }
//                "52bed620-d56e-11ee-a506-0242ac120002" -> { // Bolus properties
//                    Log.d(TAG, "Updating Bolus properties: $updateMap")
//                    bleDataRepository.updateBolusProperty(updateMap)
//                }
//                else -> { // Default for other characteristics
//                    Log.d(TAG, "Updating Other properties: $updateMap")
//                    bleDataRepository.updateProperty(updateMap)
//                }
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "Error updating repository: ${e.message}")
//        }
//    }
//}


//fun startPeriodicReading(bluetoothGatt: BluetoothGatt) {
//    val serviceGroups = characteristicMap.keys.groupBy { serviceMap[it] ?: "Unknown" }
//    var currentService: String? = serviceGroups.keys.firstOrNull()
//    var currentIndex = 0
//
//    mainHandler.post(object : Runnable {
//        @SuppressLint("MissingPermission")
//        override fun run() {
//            if (bluetoothGatt != null && currentService != null) {
//                val uuids = serviceGroups[currentService] ?: emptyList()
//                if (currentIndex < uuids.size) {
//                    val uuid = uuids[currentIndex]
//                    val characteristic = bluetoothGatt.services
//                        ?.flatMap { it.characteristics }
//                        ?.find { it.uuid.toString().lowercase() == uuid }
//                    if (characteristic != null) {
//                        bluetoothGatt.readCharacteristic(characteristic)
//                        Log.d("BLELOgs", "Reading characteristic $uuid (Service: $currentService) at ${System.currentTimeMillis()}")
//                    } else {
//                        Log.w("BLELOgs", "Characteristic $uuid not found in services")
//                    }
//
//                    currentIndex = (currentIndex + 1) % uuids.size
//                    if (currentIndex == 0) {
//                        currentService = serviceGroups.keys.find { it > currentService!! } ?: serviceGroups.keys.first()
//                    }
//                } else {
//                    currentIndex = 0
//                    currentService = serviceGroups.keys.find { it > currentService!! } ?: serviceGroups.keys.first()
//                }
//            } else if (bluetoothGatt == null) {
//                Log.w("BLELOgs", "BluetoothGatt is null, stopping periodic reading")
//                return
//            }
//
//            // Schedule the next read after 5 seconds (5000 ms)
//            mainHandler.postDelayed(this, 5000)
//        }
//    })
//}