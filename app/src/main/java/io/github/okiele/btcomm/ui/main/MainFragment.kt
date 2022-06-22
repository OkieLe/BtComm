package io.github.okiele.btcomm.ui.main

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.boopited.droidbt.CentralManager
import io.github.okiele.btcomm.R
import io.github.okiele.btcomm.bt.DataDealer
import io.github.okiele.btcomm.bt.GattClientManager
import io.github.okiele.btcomm.utils.BTConstants
import io.github.okiele.btcomm.utils.Gesture

class MainFragment : Fragment(), CentralManager.DeviceCallback, DataDealer<Gesture> {

    companion object {
        private const val TAG = "MainFragment"
        fun newInstance() = MainFragment()
    }

    private var centralManager: CentralManager? = null
    private var gattClient: GattClientManager? = null
    private val targetDevices: MutableSet<String> = mutableSetOf()

    private val viewModel: BluetoothViewModel by viewModels()
    private lateinit var rootView: View
    private lateinit var userMessage: View
    private lateinit var devicesList: RecyclerView
    private lateinit var devicesAdapter: DeviceAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.main_fragment, container, false)
        initViews(view)
        centralManager =
            CentralManager(requireContext(), this,
                BTConstants.SERVICE_GESTURE, BTConstants.BT_NAME_PREFIX)
        return view
    }

    private fun initViews(view: View) {
        rootView = view.findViewById(R.id.main)
        userMessage = view.findViewById(R.id.user_message)
        devicesList = view.findViewById(R.id.devices_list)
        rootView.setOnClickListener {
            targetDevices.clear()
            devicesAdapter.clearDevices()
            centralManager?.start()
            userMessage.visibility = View.GONE
            devicesList.visibility = View.VISIBLE
        }
        devicesList.layoutManager = LinearLayoutManager(context)
        devicesAdapter = DeviceAdapter()
        devicesList.adapter = devicesAdapter
    }

    override fun onDeviceFound(device: BluetoothDevice) {
        if ((device.type == BluetoothDevice.DEVICE_TYPE_DUAL
                    || device.type == BluetoothDevice.DEVICE_TYPE_LE) && targetDevices.add(device.address)) {
            Log.i(TAG, "${device.name}: (${device.address})@${device.type}")
            devicesAdapter.addDevice(device)
        }
    }

    override fun onComplete() {
        gattClient = GattClientManager(requireContext(), targetDevices.toList(), this)
        gattClient?.start()
    }

    override fun onFailed(error: Int) {
        Toast.makeText(requireActivity(), "Error: $error", Toast.LENGTH_LONG).show()
    }

    override fun onData(data: Gesture) {
        Toast.makeText(requireContext(), "${data.type}", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        gattClient?.stop()
        centralManager?.stop()
        super.onDestroyView()
    }

    private class DeviceAdapter: RecyclerView.Adapter<DeviceViewHolder>() {

        private val devices: MutableList<BluetoothDevice> = mutableListOf()

        fun setDevices(list: List<BluetoothDevice>) {
            devices.clear()
            devices.addAll(list)
            notifyDataSetChanged()
        }

        fun clearDevices() {
            devices.clear()
            notifyDataSetChanged()
        }

        fun addDevice(device: BluetoothDevice) {
            devices.add(device)
            notifyItemInserted(devices.size - 1)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return DeviceViewHolder(view)
        }

        override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
            holder.bind(devices[position])
        }

        override fun getItemCount(): Int {
            return devices.size
        }
    }

    private class DeviceViewHolder(view: View): RecyclerView.ViewHolder(view) {

        fun bind(device: BluetoothDevice) {
            val name = device.name + ":" + device.address
            itemView.findViewById<TextView>(android.R.id.text1).text = name
            val hint = "${device.bluetoothClass.deviceClass}@${device.type}"
            itemView.findViewById<TextView>(android.R.id.text2).text = hint
        }
    }
}
