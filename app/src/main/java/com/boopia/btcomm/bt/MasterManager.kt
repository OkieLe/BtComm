package com.boopia.btcomm.bt

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import com.boopia.btcomm.bt.scanner.ClassicDeviceScanner
import com.boopia.btcomm.bt.scanner.LeDeviceScanner
import com.boopia.btcomm.bt.scanner.ResultCallback

class MasterManager(context: Context,
                    private var scanCallback: ScanCallback? = null)
    : BaseManager(context), ResultCallback {

    interface ScanCallback {
        fun onDeviceFound(device: BluetoothDevice)
        fun onFailed(error: Int)
    }

    private var btScanner: ClassicDeviceScanner = ClassicDeviceScanner(
        context,
        this
    )
    private var leScanner: LeDeviceScanner = LeDeviceScanner(
        bluetoothAdapter.bluetoothLeScanner,
        this
    )


    private val leDevices: MutableSet<BluetoothDevice> = mutableSetOf()

    override fun onBluetoothEnabled(enable: Boolean) {
    }

    override fun start() {
        super.start()
        btScanner.startScan()
        leScanner.startScan()
    }

    override fun stop() {
        btScanner.stopScan()
        leScanner.stopScan()
        super.stop()
    }

    override fun onDeviceFound(device: BluetoothDevice) {
        if (leDevices.add(device)) {
            Log.i("BluetoothF", "${device.name}: (${device.address})@${device.type}")
            scanCallback?.onDeviceFound(device)
        }
    }

    override fun onScanComplete(type: Int) {
        Log.i(TAG, "Scan complete $type")
    }

    override fun onScanFailed(type: Int, code: Int) {
        Log.e(TAG, "Error: $type: $code")
        scanCallback?.onFailed(code)
    }

    companion object {
        private const val TAG = "MasterManager"
    }
}