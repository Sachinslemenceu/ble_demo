package com.slemenceu.blehumidityapp.data.models

sealed class OTPState {
    object OTPVerificationSuccessfull: OTPState()
    object OTPVerificationFailed: OTPState()
    object OTPNotVerified: OTPState()
}