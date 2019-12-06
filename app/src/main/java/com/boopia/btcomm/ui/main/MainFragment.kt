package com.boopia.btcomm.ui.main

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.boopia.btcomm.R
import com.boopia.btcomm.bt.GattClientManager
import com.boopia.btcomm.utils.BTConstants
import io.github.boopited.droidbt.MasterManager

class MainFragment : Fragment(), MasterManager.DeviceCallback {

    companion object {
        private const val TAG = "MainFragment"
        fun newInstance() = MainFragment()
    }

    private var masterManager: MasterManager? = null
    private var gattClient: GattClientManager? = null
    private val targetDevices: MutableSet<String> = mutableSetOf()

    private lateinit var viewModel: BluetoothViewModel
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
        masterManager =
            MasterManager(requireContext(), this, BTConstants.SERVICE_CHAT)
        return view
    }

    private fun initViews(view: View) {
        rootView = view.findViewById(R.id.main)
        userMessage = view.findViewById(R.id.user_message)
        devicesList = view.findViewById(R.id.devices_list)
        rootView.setOnClickListener {
            targetDevices.clear()
            devicesAdapter.clearDevices()
            masterManager?.start()
            userMessage.visibility = View.GONE
            devicesList.visibility = View.VISIBLE
        }
        devicesList.layoutManager = LinearLayoutManager(context)
        devicesAdapter = DeviceAdapter()
        devicesList.adapter = devicesAdapter
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(requireActivity()).get(BluetoothViewModel::class.java)
    }

    override fun onDeviceFound(device: BluetoothDevice) {
        if ((device.type == BluetoothDevice.DEVICE_TYPE_DUAL
                    || device.type == BluetoothDevice.DEVICE_TYPE_LE) && targetDevices.add(device.address)) {
            Log.i(TAG, "${device.name}: (${device.address})@${device.type}")
            devicesAdapter.addDevice(device)
        }
    }

    override fun onComplete() {
        gattClient = GattClientManager(requireContext(), targetDevices.toList())
        gattClient?.start()
    }

    override fun onFailed(error: Int) {
        Toast.makeText(requireActivity(), "Error: $error", Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        gattClient?.stop()
        masterManager?.stop()
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
