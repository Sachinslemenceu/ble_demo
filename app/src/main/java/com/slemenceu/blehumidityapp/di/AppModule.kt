package com.slemenceu.blehumidityapp.di

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import com.slemenceu.blehumidityapp.data.repos.InsufloReceiverManagerImpl
import com.slemenceu.blehumidityapp.domain.InsufloReceiverManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideBluetoothAdapter(@ApplicationContext context: Context): BluetoothAdapter {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return manager.adapter
    }

    @Provides
    @Singleton
    fun provideInsufloReceiverManager(
        @ApplicationContext context: Context,
        bluetoothAdapter: BluetoothAdapter
    ): InsufloReceiverManager {
        return InsufloReceiverManagerImpl(context, bluetoothAdapter)
    }
}