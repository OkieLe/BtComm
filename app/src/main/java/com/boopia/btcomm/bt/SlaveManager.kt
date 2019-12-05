package com.boopia.btcomm.bt

import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import com.boopia.btcomm.utils.BTConstants

class SlaveManager(context: Context): BaseManager(context) {

    private var isAdvertising = false

    fun isAdvertising(): Boolean {
        return isAdvertising
    }

    override fun start() {
        super.start()
        if (bluetoothAdapter.isEnabled) {
            startAdvertising()
        }
    }

    override fun stop() {
        if (bluetoothAdapter.isEnabled) {
            stopAdvertising()
        }
        super.stop()
    }

    override fun onBluetoothEnabled(enable: Boolean) {
        if (!enable) {
            stopAdvertising()
            isAdvertising = false
        }
    }

    /**
     * Callback to receive information about the advertisement process.
     */
    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.i(TAG, "LE Advertise Started.")
            isAdvertising = true
        }

        override fun onStartFailure(errorCode: Int) {
            isAdvertising = false
            Log.w(TAG, "LE Advertise Failed: $errorCode")
        }
    }

    /**
     * Begin advertising over Bluetooth that this device is connectable
     * and supports the Chat Service.
     */
    private fun startAdvertising() {
        val bluetoothLeAdvertiser: BluetoothLeAdvertiser? =
            bluetoothAdapter.bluetoothLeAdvertiser

        bluetoothLeAdvertiser?.let {
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build()

            val data = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(ParcelUuid(BTConstants.SERVICE_CHAT))
                .build()

            it.startAdvertising(settings, data, advertiseCallback)
        } ?: Log.w(TAG, "Failed to create advertiser")
    }

    /**
     * Stop Bluetooth advertisements.
     */
    private fun stopAdvertising() {
        val bluetoothLeAdvertiser: BluetoothLeAdvertiser? =
            bluetoothAdapter.bluetoothLeAdvertiser
        bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
            ?: Log.w(TAG, "Failed to create advertiser")
    }

    companion object {
        private const val TAG = "SlaveManager"
    }
}