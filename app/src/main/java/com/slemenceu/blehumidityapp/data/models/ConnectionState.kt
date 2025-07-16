package com.slemenceu.blehumidityapp.data.models

sealed interface ConnectionState {
    object Connected: ConnectionState
    object Disconnected: ConnectionState
    object Uninitialized: ConnectionState
    object CurrentlyInitializing: ConnectionState
}