package com.slemenceu.blehumidityapp

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.slemenceu.blehumidityapp.domain.InsufloReceiverManager
import com.slemenceu.blehumidityapp.ui.theme.BLEHumidityAppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var bluetoothAdapter: BluetoothAdapter

    @Inject
    lateinit var insufloReceiverManager: InsufloReceiverManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BLEHumidityAppTheme {
               AppNavGraph()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        insufloReceiverManager.disconnect()

    }



//    private fun showBluetoothDialog() {
//        if(!bluetoothAdapter.isEnabled){
//            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//            bluetoothLauncherIntent.launch(enableBluetoothIntent)
//        }
//    }
//
//    val bluetoothLauncherIntent = registerForActivityResult(
//        contract = ActivityResultContracts.StartActivityForResult(),
//        callback = ActivityResultCallback {
//            if (it.resultCode != Activity.RESULT_OK) {
//                showBluetoothDialog()
//            }
//        }
//    )

}

