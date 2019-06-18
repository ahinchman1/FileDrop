package com.example.resumeblu

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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

            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    val device: BluetoothDevice =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    val deviceName = device.name
                    val deviceHardwareAddress = device.address // MAC address
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        button_enable_disable.setOnClickListener {
            Timber.d("$TAG onClick: Enabling/Disabling Button")
            enableDisableButton()
        }
    }

    private fun enableDisableButton() {
        if (bluetoothAdapter == null) {
            Timber.d("$TAG enabledDisabledButton: Does not have Bluetooth capabilities")
        }

        bluetoothAdapter?.let {

            if (!it.isEnabled) {
                button_enable_disable.text = getString(R.string.disable_bluetooth)
                Timber.d("$TAG enabling Bluetooth")
                val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivity(enableBluetoothIntent)

                val bluetoothIntentDisabled = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
                registerReceiver(broadcastReceiver, bluetoothIntentDisabled)
            }

            if (it.isEnabled) {
                button_enable_disable.text = getString(R.string.enable_bluetooth)
                Timber.d("$TAG disabling Bluetooth")
                it.disable()

                val bluetoothIntentEnabled = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
                registerReceiver(broadcastReceiver, bluetoothIntentEnabled)
            }
        }
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
