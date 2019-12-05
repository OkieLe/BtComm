package com.boopia.btcomm.bt.scanner

interface Scanner {
    fun getType(): Int
    fun isScanning(): Boolean
    fun startScan()
    fun stopScan()
}