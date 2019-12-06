package com.boopia.btcomm.bt

import android.content.Context
import android.util.Log
import android.os.Bundle
import com.boopia.btcomm.utils.BTConstants
import com.boopia.btcomm.utils.BTConstants.createChatService
import com.boopia.btcomm.utils.BTConstants.wrapMessage
import com.boopia.btcomm.utils.Gesture
import com.boopia.btcomm.utils.UNKNOWN
import io.github.boopited.droidbt.common.BaseManager
import io.github.boopited.droidbt.gatt.GattServer
import java.util.*

class GattServerManager(context: Context): BaseManager(context), GattServer.GattServerCallback {

    private var bluetoothGattServer: GattServer = GattServer(context, this)

    private val pendingMessage = mutableListOf<Gesture>()

    override fun onBluetoothEnabled(enable: Boolean) {
        if (!enable) {
            bluetoothGattServer.shutdown()
        }
    }

    override fun isNotification(uuid: UUID): Boolean {
        return uuid == BTConstants.CONTENT_NOTIFY
    }

    override fun getCharacteristic(uuid: UUID): ByteArray? {
        return when (uuid) {
            BTConstants.CHARACTERISTIC_GESTURE -> {
                Log.i(TAG, "Read message")
                wrapMessage(pendingMessage.first())
            }
            else -> {
                // Invalid characteristic
                Log.w(TAG, "Invalid Characteristic Read: $uuid")
                null
            }
        }
    }

    override fun setCharacteristic(uuid: UUID, value: ByteArray): Boolean {
        return true
    }

    override fun getDescriptor(uuid: UUID): ByteArray? {
        return null
    }

    override fun setDescriptor(uuid: UUID, value: ByteArray): Boolean {
        return true
    }

    override fun start() {
        super.start()
        if (bluetoothAdapter.isEnabled) {
            bluetoothGattServer.startService(createChatService())
        }
    }

    override fun stop() {
        if (bluetoothAdapter.isEnabled) {
            bluetoothGattServer.stopService(BTConstants.SERVICE_CHAT)
        }
        bluetoothGattServer.shutdown()
        super.stop()
    }

    /**
     * Send a chat service notification to any devices that are subscribed
     * to the characteristic.
     */
    fun notifyGesture(uuid: UUID, extra: Bundle?) {
        Log.i(TAG, "Sending update to subscribers")
        bluetoothGattServer.notifyDevices(
            BTConstants.SERVICE_CHAT, uuid,
            wrapNotificationData(uuid, extra))
    }

    private fun wrapNotificationData(characteristic: UUID, extra: Bundle?): ByteArray {
        return when (characteristic) {
            BTConstants.CHARACTERISTIC_GESTURE -> {
                val time = extra?.getLong(BTConstants.EXTRA_TIME_STAMP) ?: System.currentTimeMillis() / 1000
                val data = extra?.getInt(BTConstants.EXTRA_DATA) ?: UNKNOWN
                wrapMessage(Gesture(data, time))
            }
            else -> ByteArray(0)
        }
    }

    companion object {
        private const val TAG = "GattServerManager"
    }
}