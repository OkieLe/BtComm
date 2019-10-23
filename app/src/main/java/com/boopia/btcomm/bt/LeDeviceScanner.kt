package com.boopia.btcomm.bt

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.*
import android.os.Handler
import android.os.ParcelUuid
import com.boopia.btcomm.utils.BTConstants

class LeDeviceScanner(private val scanner: BluetoothLeScanner,
                      private val resultCallback: ResultCallback) {

    interface ResultCallback {
        fun onScanFailed(code: Int)
        fun onDeviceFound(device: BluetoothDevice)
        fun onScanComplete()
    }

    private val handler = Handler()
    private var isScanning: Boolean = false

    private val scanFilter = ScanFilter.Builder()
        .setServiceUuid(ParcelUuid(BTConstants.SERVICE_CHAT))
        .build()
    private val scannerCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.device?.let { resultCallback.onDeviceFound(it) }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            results?.map {
                resultCallback.onDeviceFound(it.device)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            isScanning = false
            resultCallback.onScanFailed(errorCode)
        }
    }

    fun isScanning() = isScanning

    fun startScan() {
        if (isScanning) return
        isScanning = true
        scanner.startScan(
            listOf(scanFilter),
            ScanSettings.Builder().build(),
            scannerCallback
        )
        handler.postDelayed({
            stopScan()
            resultCallback.onScanComplete()
        }, 10000)
    }

    fun stopScan() {
        if (!isScanning) return
        scanner.flushPendingScanResults(scannerCallback)
        scanner.stopScan(scannerCallback)
        isScanning = false
    }
}