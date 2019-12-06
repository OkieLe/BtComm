package com.boopia.btcomm.utils

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import java.util.*

object BTConstants {

    val SERVICE_CHAT: UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
    val CHARACTERISTIC_GESTURE: UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
    val CONTENT_NOTIFY: UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")

    const val ACTION_GATT_CONNECTED = "com.thoughtworks.hmi.ACTION_GATT_CONNECTED"
    const val ACTION_GATT_DISCONNECTED = "com.thoughtworks.hmi.ACTION_GATT_DISCONNECTED"
    const val ACTION_GATT_SERVICES_DISCOVERED = "com.thoughtworks.hmi.ACTION_GATT_SERVICES_DISCOVERED"
    const val ACTION_DATA_AVAILABLE = "com.thoughtworks.hmi.ACTION_DATA_AVAILABLE"

    const val EXTRA_DATA = "com.thoughtworks.hmi.EXTRA_DATA"
    const val EXTRA_TIME_STAMP = "com.thoughtworks.hmi.EXTRA_TIME"

    const val ACTION_MESSAGE_SENT = "com.thoughtworks.hmi.ACTION_MESSAGE_SENT"

    fun createChatService(): BluetoothGattService {
        val service = BluetoothGattService(
            SERVICE_CHAT,
            BluetoothGattService.SERVICE_TYPE_PRIMARY)

        val message = BluetoothGattCharacteristic(
            CHARACTERISTIC_GESTURE,
            //Read-only characteristic, supports notifications
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ)
        val content = BluetoothGattDescriptor(
            CONTENT_NOTIFY,
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE)
        message.addDescriptor(content)
        service.addCharacteristic(message)

        return service
    }

    fun wrapMessage(gesture: Gesture): ByteArray {
        return byteArrayOf(gesture.type.toByte())
            .plus(longToUInt32ByteArray(gesture.date))
    }

    private fun longToUInt32ByteArray(value: Long): ByteArray {
        val bytes = ByteArray(4)
        bytes[0] = (value and 0xFF).toByte()
        bytes[1] = (value shr 8 and 0xFF).toByte()
        bytes[2] = (value shr 16 and 0xFF).toByte()
        bytes[3] = (value shr 24 and 0xFF).toByte()
        return bytes
    }

    @ExperimentalUnsignedTypes
    fun unwrapMessage(value: ByteArray): Gesture {
        val type = value[0].toInt()
        val time = if (value.size > 5) {
            uint32ByteArrayToLong(value.copyOfRange(1, 5))
        } else System.currentTimeMillis() / 1000
        return Gesture(type, time)
    }

    @ExperimentalUnsignedTypes
    private fun uint32ByteArrayToLong(value: ByteArray): Long {
        if (value.size != 4) throw IllegalArgumentException("Wrong size byte array")
        return ((value[3].toUInt() and 0xFFU) shl 8 or
                (value[2].toUInt() and 0xFFU) shl 8 or
                (value[1].toUInt() and 0xFFU) shl 8 or
                (value[0].toUInt() and 0xFFU)).toLong()
    }
}