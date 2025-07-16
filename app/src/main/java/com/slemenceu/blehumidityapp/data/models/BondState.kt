package com.slemenceu.blehumidityapp.data.models

import java.util.Objects

sealed class BondState{
    object Bonded: BondState()
    object None: BondState()
    object Bonding: BondState()
    object Unchecked: BondState()
}
