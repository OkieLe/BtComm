package com.boopia.btcomm.bt.scanner

import android.bluetooth.BluetoothDevice

interface ResultCallback {
    fun onScanFailed(type: Int, code: Int)
    fun onDeviceFound(device: BluetoothDevice)
    fun onScanComplete(type: Int)
}