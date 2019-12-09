package com.boopia.btcomm.bt

import android.bluetooth.*
import android.content.Context
import android.util.Log
import com.boopia.btcomm.utils.BTConstants
import com.boopia.btcomm.utils.BTConstants.unwrapMessage
import com.boopia.btcomm.utils.Gesture
import io.github.boopited.droidbt.common.BaseManager
import io.github.boopited.droidbt.gatt.GattClient

interface DataDealer<T> {
    fun onData(data: T)
}

class GattClientManager(
    context: Context,
    private val devicesAddress: List<String>,
    private val dataDealer: DataDealer<Gesture>
): BaseManager(context), GattClient.GattClientCallback {

    private var bluetoothGattClients: MutableSet<GattClient> = mutableSetOf()

    override fun onGattConnected(gatt: BluetoothGatt) {
        Log.i(TAG, "Connected to GATT server.")
    }

    override fun onGattDisconnected(gatt: BluetoothGatt) {
        Log.i(TAG, "Disconnected from GATT server.")
    }

    override fun onServiceDiscovered(gatt: BluetoothGatt) {
        bluetoothGattClients.find { it.sameAs(gatt) }
            ?.setCharacteristicNotification(
                gatt.getService(BTConstants.SERVICE_GESTURE)
                    .getCharacteristic(BTConstants.CHARACTERISTIC_GESTURE),
                BTConstants.CONTENT_NOTIFY, true)
    }

    override fun onDataAvailable(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        val data = unwrapMessage(characteristic.value)
        Log.i(TAG, data.toString())
        dataDealer.onData(data)
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