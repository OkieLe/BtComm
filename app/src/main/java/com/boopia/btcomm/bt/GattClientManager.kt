package com.boopia.btcomm.bt

import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.util.Log
import com.boopia.btcomm.utils.BTConstants
import com.boopia.btcomm.utils.BTConstants.unwrapMessage
import io.github.boopited.droidbt.common.BaseManager
import io.github.boopited.droidbt.gatt.GattClient

class GattClientManager(
    context: Context,
    private val devicesAddress: List<String>
): BaseManager(context), GattClient.GattClientCallback {

    private var bluetoothGattClients: MutableSet<GattClient> = mutableSetOf()

    override fun onGattConnected(gatt: BluetoothGatt) {
        Log.i(TAG, "Connected to GATT server.")
        val intentAction: String = BTConstants.ACTION_GATT_CONNECTED
        broadcastUpdate(intentAction)
    }

    override fun onGattDisconnected(gatt: BluetoothGatt) {
        val intentAction = BTConstants.ACTION_GATT_DISCONNECTED
        Log.i(TAG, "Disconnected from GATT server.")
        broadcastUpdate(intentAction)
    }

    override fun onServiceDiscovered(gatt: BluetoothGatt) {
        broadcastUpdate(BTConstants.ACTION_GATT_SERVICES_DISCOVERED)
        bluetoothGattClients.find { it.sameAs(gatt) }
            ?.setCharacteristicNotification(
                gatt.getService(BTConstants.SERVICE_GESTURE)
                    .getCharacteristic(BTConstants.CHARACTERISTIC_GESTURE),
                BTConstants.CONTENT_NOTIFY, true)
    }

    override fun onDataAvailable(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        broadcastUpdate(BTConstants.ACTION_DATA_AVAILABLE, characteristic)
        Log.i(TAG, unwrapMessage(characteristic.value).toString())
    }

    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        context.sendBroadcast(intent)
    }

    private fun broadcastUpdate(action: String, characteristic: BluetoothGattCharacteristic) {
        val intent = Intent(action)

        if (BTConstants.CHARACTERISTIC_GESTURE == characteristic.uuid) {
            // For all other profiles, writes the data formatted in HEX.
            val data = characteristic.value
            if (data != null) {
                val type = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                intent.putExtra(BTConstants.EXTRA_DATA, type)
                val timestamp = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 1)
                intent.putExtra(BTConstants.EXTRA_TIME_STAMP, timestamp.toLong())
            }
        }
        context.sendBroadcast(intent)
    }

    override fun onBluetoothEnabled(enable: Boolean) {
        if (!enable) stop()
    }

    override fun start() {
        super.start()
        devicesAddress.forEach {
            val client = GattClient(context, this)
            client.connect(it)
            bluetoothGattClients.add(client)
        }
    }

    override fun stop() {
        bluetoothGattClients.forEach { it.disconnect() }
        super.stop()
    }

    companion object {
        private const val TAG = "GattClientManager"
    }
}