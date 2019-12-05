package com.boopia.btcomm.bt

import android.bluetooth.*
import android.content.Context
import android.util.Log
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattCharacteristic
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import com.boopia.btcomm.utils.BTConstants.createChatService
import com.boopia.btcomm.utils.BTConstants.wrapMessage
import com.boopia.btcomm.utils.BTConstants.wrapOnlineState
import com.boopia.btcomm.model.Message
import com.boopia.btcomm.model.MessageType
import com.boopia.btcomm.utils.BTConstants
import java.util.*

class GattServerManager(context: Context): BaseManager(context) {

    /* Bluetooth API */
    private var bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private var bluetoothGattServer: BluetoothGattServer? = null

    /* Collection of notification subscribers */
    private val registeredDevices = mutableSetOf<BluetoothDevice>()

    private val pendingMessage = mutableListOf<Message>()

    /**
     * Listens for local chat actions and triggers a notification to
     * Bluetooth subscribers.
     */
    private val chatReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BTConstants.ACTION_ONLINE_STATE -> {
                    notifyRegisteredDevice(BTConstants.CHARACTERISTIC_ONLINE_STATE, intent.extras)
                }
                BTConstants.ACTION_SEND_MESSAGE -> {
                    notifyRegisteredDevice(BTConstants.CHARACTERISTIC_MESSAGE, intent.extras)
                }
                BTConstants.ACTION_MESSAGE_RECEIVED -> {
                    updateLocalUi()
                }
            }
        }
    }

    override fun onBluetoothEnabled(enable: Boolean) {
        if (!enable) {
            stopServer()
        }
    }

    /**
     * Callback to handle incoming requests to the GATT server.
     * All read/write requests for characteristics and descriptors are handled here.
     */
    private val gattServerCallback = object : BluetoothGattServerCallback() {

        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "BluetoothDevice CONNECTED: $device")
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "BluetoothDevice DISCONNECTED: $device")
                //Remove device from any active subscriptions
                registeredDevices.remove(device)
            }
        }

        override fun onCharacteristicReadRequest(device: BluetoothDevice, requestId: Int, offset: Int,
                                                 characteristic: BluetoothGattCharacteristic) {
            when (characteristic.uuid) {
                BTConstants.CHARACTERISTIC_MESSAGE -> {
                    Log.i(TAG, "Read message")
                    bluetoothGattServer?.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        wrapMessage(pendingMessage.first()))
                }
                BTConstants.CHARACTERISTIC_ONLINE_STATE -> {
                    Log.i(TAG, "Read online state")
                    bluetoothGattServer?.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        wrapOnlineState(true))
                }
                else -> {
                    // Invalid characteristic
                    Log.w(TAG, "Invalid Characteristic Read: " + characteristic.uuid)
                    bluetoothGattServer?.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        null)
                }
            }
        }

        override fun onDescriptorReadRequest(device: BluetoothDevice, requestId: Int, offset: Int,
                                             descriptor: BluetoothGattDescriptor) {
            when (descriptor.uuid) {
                BTConstants.CONTENT_NOTIFY -> {
                    Log.d(TAG, "Content descriptor read")
                    val returnValue = if (registeredDevices.contains(device)) {
                        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    } else {
                        BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                    }
                    bluetoothGattServer?.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        returnValue)
                }
                else -> {
                    Log.w(TAG, "Unknown descriptor read request")
                    bluetoothGattServer?.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0, null)
                }
            }
        }

        override fun onDescriptorWriteRequest(device: BluetoothDevice, requestId: Int,
                                              descriptor: BluetoothGattDescriptor,
                                              preparedWrite: Boolean, responseNeeded: Boolean,
                                              offset: Int, value: ByteArray) {
            if (BTConstants.CONTENT_NOTIFY == descriptor.uuid) {
                if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, value)) {
                    Log.d(TAG, "Subscribe device to notifications: $device")
                    registeredDevices.add(device)
                } else if (Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, value)) {
                    Log.d(TAG, "Unsubscribe device from notifications: $device")
                    registeredDevices.remove(device)
                }

                if (responseNeeded) {
                    bluetoothGattServer?.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0, null)
                }
            } else {
                Log.w(TAG, "Unknown descriptor write request")
                if (responseNeeded) {
                    bluetoothGattServer?.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0, null)
                }
            }
        }
    }

    override fun start() {
        super.start()
        // Register for system clock events
        val chatFilter = IntentFilter().apply {
            addAction(BTConstants.ACTION_ONLINE_STATE)
            addAction(BTConstants.ACTION_SEND_MESSAGE)
            addAction(BTConstants.ACTION_MESSAGE_RECEIVED)
        }
        context.registerReceiver(chatReceiver, chatFilter)

        if (bluetoothAdapter.isEnabled) {
            startServer()
        }
    }

    override fun stop() {
        if (bluetoothAdapter.isEnabled) {
            stopServer()
        }

        context.unregisterReceiver(chatReceiver)
        super.stop()
    }

    /**
     * Initialize the GATT server instance with the services/characteristics
     * from the Chat Service.
     */
    private fun startServer() {
        bluetoothGattServer = bluetoothManager.openGattServer(context, gattServerCallback)

        bluetoothGattServer?.addService(createChatService())
            ?: Log.w(TAG, "Unable to create GATT server")
    }

    /**
     * Shut down the GATT server.
     */
    private fun stopServer() {
        bluetoothGattServer?.close()
    }

    /**
     * Send a chat service notification to any devices that are subscribed
     * to the characteristic.
     */
    private fun notifyRegisteredDevice(uuid: UUID, extra: Bundle?) {
        if (registeredDevices.isEmpty()) {
            Log.i(TAG, "No subscribers registered")
            return
        }
        Log.i(TAG, "Sending update to ${registeredDevices.size} subscribers")
        for (device in registeredDevices) {
            val message = bluetoothGattServer
                ?.getService(BTConstants.SERVICE_CHAT)
                ?.getCharacteristic(uuid)
            message?.value = wrapNotificationData(uuid, extra)
            bluetoothGattServer?.notifyCharacteristicChanged(device, message, false)
        }
    }

    private fun wrapNotificationData(characteristic: UUID, extra: Bundle?): ByteArray {
        return when (characteristic) {
            BTConstants.CHARACTERISTIC_MESSAGE -> {
                val time = extra?.getLong(BTConstants.EXTRA_TIME_STAMP) ?: System.currentTimeMillis() / 1000
                val data = extra?.getString(BTConstants.EXTRA_DATA).orEmpty()
                val type = extra?.getInt(BTConstants.EXTRA_DATA_TYPE)
                wrapMessage(Message(MessageType.typeOf(type), data, time))
            }
            BTConstants.CHARACTERISTIC_ONLINE_STATE -> {
                val state = extra?.getBoolean(BTConstants.EXTRA_DATA) ?: false
                wrapOnlineState(state)
            }
            else -> ByteArray(0)
        }
    }

    /**
     * Update graphical UI on devices that support it with the current chat.
     */
    private fun updateLocalUi() {
    }

    companion object {
        private const val TAG = "GattServerManager"
    }
}