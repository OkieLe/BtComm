package com.boopia.btcomm.utils

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import com.boopia.btcomm.model.Message
import java.util.*

object BTConstants {

    val SERVICE_CHAT: UUID = UUID.fromString("703ecd36-825e-4d0f-b200-ae901ff8decb")
    val CHARACTERISTIC_MESSAGE: UUID = UUID.fromString("a8089ebe-8d56-4b04-8896-b925c3bf7c18")
    val CHARACTERISTIC_ONLINE_STATE: UUID = UUID.fromString("a7a3b83d-f2d4-48a6-a7d0-a08d9a3f417a")
    val CONTENT_NOTIFY: UUID = UUID.fromString("385aa50f-81a4-4fc2-a663-d5cc22d72c84")

    const val MAX_MESSAGE_LENGTH = 16

    const val ACTION_GATT_CONNECTED = "com.boopia.btcomm.ACTION_GATT_CONNECTED"
    const val ACTION_GATT_DISCONNECTED = "com.boopia.btcomm.ACTION_GATT_DISCONNECTED"
    const val ACTION_GATT_SERVICES_DISCOVERED = "com.boopia.btcomm.ACTION_GATT_SERVICES_DISCOVERED"
    const val ACTION_DATA_AVAILABLE = "com.boopia.btcomm.ACTION_DATA_AVAILABLE"

    const val EXTRA_DATA = "com.boopia.btcomm.EXTRA_DATA"
    const val EXTRA_DATA_TYPE = "com.boopia.btcomm.EXTRA_DATA_TYPE"
    const val EXTRA_TIME_STAMP = "com.boopia.btcomm.EXTRA_TIME"

    const val ACTION_ONLINE_STATE = "com.boopia.btcomm.ACTION_ONLINE_STATE"
    const val ACTION_SEND_MESSAGE = "com.boopia.btcomm.ACTION_SEND_MESSAGE"
    const val ACTION_MESSAGE_RECEIVED = "com.boopia.btcomm.ACTION_MESSAGE_RECEIVED"

    fun createChatService(): BluetoothGattService {
        val service = BluetoothGattService(
            SERVICE_CHAT,
            BluetoothGattService.SERVICE_TYPE_PRIMARY)

        val message = BluetoothGattCharacteristic(
            CHARACTERISTIC_MESSAGE,
            //Read-only characteristic, supports notifications
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ)
        val content = BluetoothGattDescriptor(
            CONTENT_NOTIFY,
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE)
        message.addDescriptor(content)
        service.addCharacteristic(message)

        val onlineState = BluetoothGattCharacteristic(
            CHARACTERISTIC_ONLINE_STATE,
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ)
        service.addCharacteristic(onlineState)

        return service
    }

    fun wrapOnlineState(online: Boolean): ByteArray {
        return byteArrayOf(if (online) 1 else 0)
    }

    fun wrapMessage(message: Message): ByteArray {
        return byteArrayOf(message.type.ordinal.toByte())
            .plus(longToUInt32ByteArray(message.date))
            .plus(message.content.toByteArray())
    }

    private fun longToUInt32ByteArray(value: Long): ByteArray {
        val bytes = ByteArray(4)
        bytes[0] = (value and 0xFF).toByte()
        bytes[1] = (value shr 8 and 0xFF).toByte()
        bytes[2] = (value shr 16 and 0xFF).toByte()
        bytes[3] = (value shr 24 and 0xFF).toByte()
        return bytes
    }
}