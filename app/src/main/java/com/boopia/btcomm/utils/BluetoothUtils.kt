package com.boopia.btcomm.utils

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent

object BluetoothUtils {
    const val REQUEST_ENABLE_BT = 1001

    fun openBluetooth(context: Activity, code: Int) {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        context.startActivityForResult(enableBtIntent, code)
    }
}