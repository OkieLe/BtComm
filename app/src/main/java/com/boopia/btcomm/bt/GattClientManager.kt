package com.boopia.btcomm.bt

import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.util.Log
import com.boopia.btcomm.bt.gatt.GattClient
import com.boopia.btcomm.utils.BTConstants

class GattClientManager(context: Context): BaseManager(context), GattClient.GattClientCallback {

    private var bluetoothGattClients: MutableSet<GattClient> = mutableSetOf()

    override fun onGattConnected() {
        super.onGattConnected()
        val intentAction: String = BTConstants.ACTION_GATT_CONNECTED
        broadcastUpdate(intentAction)
    }

    override fun onGattDisconnected() {
        val intentAction = BTConstants.ACTION_GATT_DISCONNECTED
        Log.i(TAG, "Disconnected from GATT server.")
        broadcastUpdate(intentAction)
        super.onGattDisconnected()
    }

    override fun onServiceDiscovered() {
        broadcastUpdate(BTConstants.ACTION_GATT_SERVICES_DISCOVERED)
    }

    override fun onDataAvailable(characteristic: BluetoothGattCharacteristic) {
        broadcastUpdate(BTConstants.ACTION_DATA_AVAILABLE, characteristic)
    }

    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        context.sendBroadcast(intent)
    }

    private fun broadcastUpdate(action: String, characteristic: BluetoothGattCharacteristic) {
        val intent = Intent(action)

        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        if (BTConstants.CHARACTERISTIC_ONLINE_STATE == characteristic.uuid) {
            val onlineState = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
            Log.d(TAG, String.format("Received online state: %d", onlineState))
            intent.putExtra(BTConstants.EXTRA_DATA, onlineState.toString())
        } else if (BTConstants.CHARACTERISTIC_MESSAGE == characteristic.uuid) {
            // For all other profiles, writes the data formatted in HEX.
            val data = characteristic.value
            if (data != null) {
                val type = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                intent.putExtra(BTConstants.EXTRA_DATA_TYPE, type)
                val timestamp = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 1)
                intent.putExtra(BTConstants.EXTRA_TIME_STAMP, timestamp.toLong())
                val content = characteristic.getStringValue(5)
                intent.putExtra(BTConstants.EXTRA_DATA, content)
            }
        }
        context.sendBroadcast(intent)
    }

    override fun onBluetoothEnabled(enable: Boolean) {
        if (!enable) stop()
    }

    override fun start() {
        super.start()

    }

    override fun stop() {
        bluetoothGattClients.forEach { it.disconnect() }
        super.stop()
    }

    companion object {
        private const val TAG = "GattClientManager"
    }
}