package com.boopia.btcomm.bt.scanner

import android.bluetooth.le.*
import android.os.Handler
import android.os.ParcelUuid
import com.boopia.btcomm.utils.BTConstants

class LeDeviceScanner(private val scanner: BluetoothLeScanner,
                      private val resultCallback: ResultCallback
): Scanner {

    private val handler = Handler()
    private var isScanning: Boolean = false

    private val chatServiceFilter = ScanFilter.Builder()
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
            resultCallback.onScanFailed(getType(), errorCode)
        }
    }

    override fun getType(): Int {
        return 2
    }

    override fun isScanning() = isScanning

    override fun startScan() {
        if (isScanning) return
        isScanning = true
        scanner.startScan(
            emptyList(),
            ScanSettings.Builder().build(),
            scannerCallback
        )
        handler.postDelayed({
            stopScan()
            resultCallback.onScanComplete(getType())
        }, 20000)
    }

    override fun stopScan() {
        if (!isScanning) return
        scanner.flushPendingScanResults(scannerCallback)
        scanner.stopScan(scannerCallback)
        isScanning = false
    }
}