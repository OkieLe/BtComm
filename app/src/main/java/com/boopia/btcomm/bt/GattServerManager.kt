package com.boopia.btcomm.bt

import android.bluetooth.*
import android.content.Context
import android.util.Log
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.ParcelUuid
import com.boopia.btcomm.utils.BTConstants.createChatService
import com.boopia.btcomm.utils.BTConstants.wrapMessage
import com.boopia.btcomm.utils.BTConstants.wrapOnlineState
import com.boopia.btcomm.model.Message
import com.boopia.btcomm.model.MessageType
import com.boopia.btcomm.utils.BTConstants
import java.util.*

class GattServerManager(private val context: Context) {

    interface GattCallback {
    }

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

    /**
     * Listens for Bluetooth adapter events to enable/disable
     * advertising and server functionality.
     */
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)) {
                BluetoothAdapter.STATE_ON -> {
                    startAdvertising()
                    startServer()
                }
                BluetoothAdapter.STATE_OFF -> {
                    stopServer()
                    stopAdvertising()
                }
            }
        }
    }

    /**
     * Callback to receive information about the advertisement process.
     */
    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.i(TAG, "LE Advertise Started.")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.w(TAG, "LE Advertise Failed: $errorCode")
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

    init {
        val bluetoothAdapter = bluetoothManager.adapter
        // We can't continue without proper Bluetooth support
        check(checkBluetoothSupport(bluetoothAdapter))

        // Register for system Bluetooth events
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(bluetoothReceiver, filter)
        if (!bluetoothAdapter.isEnabled) {
            Log.d(TAG, "Bluetooth is currently disabled...enabling")
            bluetoothAdapter.enable()
        } else {
            Log.d(TAG, "Bluetooth enabled...starting services")
            startAdvertising()
            startServer()
        }
    }

    fun start() {
        // Register for system clock events
        val filter = IntentFilter().apply {
            addAction(BTConstants.ACTION_ONLINE_STATE)
            addAction(BTConstants.ACTION_SEND_MESSAGE)
            addAction(BTConstants.ACTION_MESSAGE_RECEIVED)
        }

        context.registerReceiver(chatReceiver, filter)
    }

    fun stop() {
        context.unregisterReceiver(chatReceiver)
    }

    fun destroy() {
        val bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter.isEnabled) {
            stopServer()
            stopAdvertising()
        }

        context.unregisterReceiver(bluetoothReceiver)
    }

    /**
     * Verify the level of Bluetooth support provided by the hardware.
     * @param bluetoothAdapter System [BluetoothAdapter].
     * @return true if Bluetooth is properly supported, false otherwise.
     */
    private fun checkBluetoothSupport(bluetoothAdapter: BluetoothAdapter?): Boolean {
        if (bluetoothAdapter == null) {
            Log.w(TAG, "Bluetooth is not supported")
            return false
        }

        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.w(TAG, "Bluetooth LE is not supported")
            return false
        }

        return true
    }

    /**
     * Begin advertising over Bluetooth that this device is connectable
     * and supports the Chat Service.
     */
    private fun startAdvertising() {
        val bluetoothLeAdvertiser: BluetoothLeAdvertiser? =
            bluetoothManager.adapter.bluetoothLeAdvertiser

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
            bluetoothManager.adapter.bluetoothLeAdvertiser
        bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
            ?: Log.w(TAG, "Failed to create advertiser")
    }

    /**
     * Initialize the GATT server instance with the services/characteristics
     * from the Chat Service.
     */
    private fun startServer() {
        bluetoothGattServer = bluetoothManager.openGattServer(context, gattServerCallback)

        bluetoothGattServer?.addService(createChatService())
            ?: Log.w(TAG, "Unable to create GATT server")

        // Initialize the local UI
        updateLocalUi()
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