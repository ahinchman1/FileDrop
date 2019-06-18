package com.example.resumeblu

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View.VISIBLE
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    var bluetoothAdapter: BluetoothAdapter? = null


    /**
     * Register to handle the broadcast when devices are discovered.
     * All that's needed from the [BluetoothDevice] object in order to initiate connection is the MAC address.
     * The MAC address can later be extracted in order to initiate the connection
     */
    private val broadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action

            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)

                when (state) {
                    BluetoothAdapter.STATE_OFF -> Timber.d("$TAG onReceive: STATE OFF")
                    BluetoothAdapter.STATE_TURNING_OFF -> Timber.d("$TAG broadcastReceiver: STATE TURNING OFF")
                    BluetoothAdapter.STATE_ON -> Timber.d("$TAG broadcastReceiver: STATE ON")
                    BluetoothAdapter.STATE_TURNING_ON -> Timber.d("$TAG broadcastReceiver: STATE TURNING ON")
                }
            }
        }
    }

    /**
     * Register to handle the broadcast when devices are discovered.
     * Discoverability mode on/off or expire
     */
    private val broadcastReceiverDiscoverable = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action

            if (action == BluetoothAdapter.ACTION_SCAN_MODE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)

                when (state) {
                    BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE ->
                        Timber.d("$TAG broadcastReceiverDiscoverable: Discoverability Enabled.")
                    BluetoothAdapter.SCAN_MODE_CONNECTABLE ->
                        Timber.d("$TAG broadcastReceiverDiscoverable: Discoverability Enabled. Able to receive connections.")
                    BluetoothAdapter.SCAN_MODE_NONE ->
                        Timber.d("$TAG broadcastReceiverDiscoverable: Discoverability Disabled. Not able to receive connections.")
                    BluetoothAdapter.STATE_CONNECTING ->
                        Timber.d("$TAG broadcastReceiver: Connecting...")
                    BluetoothAdapter.STATE_CONNECTED ->
                        Timber.d("$TAG broadcastReceiver: Connected.")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        button_enable_disable_bluetooth.setOnClickListener {
            Timber.d("$TAG onClick: Enabling/Disabling Button")
            enableDisableBluetooth()
        }

        button_enable_disable_discoverable.setOnClickListener {
            Timber.d("$TAG onClick: Enabling/Disabling Device Discovery")
            enableDisableDeviceDiscovery()
        }
    }

    private fun enableDisableBluetooth() {
        if (bluetoothAdapter == null) {
            Timber.d("$TAG enabledDisabledButton: Does not have Bluetooth capabilities")
        }

        bluetoothAdapter?.let {

            if (!it.isEnabled) {
                button_enable_disable_bluetooth.text = getString(R.string.disable_bluetooth)
                Timber.d("$TAG enabling Bluetooth")
                val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivity(enableBluetoothIntent)

                val bluetoothIntentDisabled = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
                registerReceiver(broadcastReceiver, bluetoothIntentDisabled)
            }

            if (it.isEnabled) {
                button_enable_disable_bluetooth.text = getString(R.string.enable_bluetooth)
                Timber.d("$TAG disabling Bluetooth")
                it.disable()

                val bluetoothIntentEnabled = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
                registerReceiver(broadcastReceiver, bluetoothIntentEnabled)
            }
        }
    }

    private fun enableDisableDeviceDiscovery() {
        Timber.d("$TAG enableDisableDeviceDiscovery: Making device discoverable for 300 seconds")

        making_device_discoverable_loading.visibility = VISIBLE
        loading_spinner.visibility = VISIBLE
        button_enable_disable_discoverable.isEnabled = false

        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)

        val intentFilter = IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)
        registerReceiver(broadcastReceiverDiscoverable, intentFilter)
    }

    override fun onDestroy() {
        Timber.d("$TAG onDestroy: called.")
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
