package com.slemenceu.blehumidityapp.data.utils

import android.content.Context
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton


object PermissionManager {
    val permissionsArray = arrayOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.BLUETOOTH_SCAN,
        android.Manifest.permission.BLUETOOTH_CONNECT,
        android.Manifest.permission.BLUETOOTH_ADVERTISE,
        android.Manifest.permission.BLUETOOTH_ADMIN,
    )
    fun checkAllPermissionsGranted(context: Context): Boolean {
        return permissionsArray.all {
            ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }
}