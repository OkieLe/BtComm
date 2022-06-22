package io.github.okiele.btcomm

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.github.boopited.droidbt.common.BluetoothUtils
import io.github.okiele.btcomm.ui.main.MainFragment

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

    private fun showBluetoothScanner() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, MainFragment.newInstance())
            .commitNow()
    }

    private fun checkBluetoothState() {
        if (!BluetoothUtils.isBluetoothEnabled(this)) {
            BluetoothUtils.openBluetooth(this) { showBluetoothScanner()}
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
