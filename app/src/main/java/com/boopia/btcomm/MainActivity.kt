package com.boopia.btcomm

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.boopia.btcomm.ui.main.MainFragment
import com.boopia.btcomm.utils.BluetoothUtils
import com.boopia.btcomm.utils.BluetoothUtils.REQUEST_ENABLE_BT

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        if (!BluetoothUtils.hasPermissions(this)) {
            requestPermissions(BluetoothUtils.permissionsToAsk(this), BluetoothUtils.REQUEST_PERMISSION)
        } else {
            checkBluetoothState()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                showBluetoothScanner()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun showBluetoothScanner() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, MainFragment.newInstance())
            .commitNow()
    }

    private fun checkBluetoothState() {
        if (!BluetoothUtils.isBluetoothEnabled()) {
            BluetoothUtils.openBluetooth(this, REQUEST_ENABLE_BT)
        } else {
            showBluetoothScanner()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == BluetoothUtils.REQUEST_PERMISSION) {
            if (grantResults.any { it != PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this,
                    R.string.missing_permission_revoked_by_user, Toast.LENGTH_LONG).show()
                finish()
            } else {
                checkBluetoothState()
            }
        }
    }
}
