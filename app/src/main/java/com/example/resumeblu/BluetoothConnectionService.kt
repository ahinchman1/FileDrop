package com.example.resumeblu

import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.*

class BluetoothConnectionService(val context: Context, var bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()) {

    private var insecureAcceptThread: AcceptThread? = null
    private var connectThread: ConnectThread? = null
    private var connectedThread: ConnectedThread? = null
    private lateinit var device: BluetoothDevice
    private lateinit var deviceUUID: UUID

    /**
     * This thread runs while listening for incoming connections.
     * It behaves like a server-side client. It runs until a
     * connection is accepted (or until cancelled)
     */
    inner class AcceptThread(var serverSocket: BluetoothServerSocket? = null): Thread() {

        var tmp: BluetoothServerSocket? = null

        init {
            try {
                tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(
                    appName, MY_UUID_INSECURE)
                Timber.d("$TAG AcceptThread: Setting up Server using: $MY_UUID_INSECURE")
            } catch (e: IOException) {
                Timber.e(e)
            }

            serverSocket = tmp
        }

        override fun run() {
            Timber.i("run:  AcceptThread Running")
            var socket: BluetoothSocket? = null

            try {
                // accept thread just hangs here until something connects
                Timber.d("$TAG run: RFCOM server socket start")

                socket = serverSocket?.accept()

                Timber.d("$TAG run: RFCOM server socket accepted connection")

            } catch (e: IOException) {
                Timber.e("$TAG AcceptThread: IOException: $e")
            }

            // TODO for the 3rd video
            if (socket != null) {
                // connected(socket, device)
            }
        }

        fun cancel() {
            Timber.d("$TAG cancel: Cancelling AcceptThread.")

            try {
                serverSocket?.close()
            } catch (e: IOException) {
                Timber.e("$TAG cancel: Close of AcceptThread ServerSocket failed. $e")
            }
        }

    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     *
     * If you have two devices, they're both going to be sitting in the [AcceptThread]
     * state until a [ConnectThread] starts.
     */
    inner class ConnectThread(private val ctDevice: BluetoothDevice, private val uuid: UUID): Thread() {

        var socket: BluetoothSocket? = null

        init {
            Timber.d("$TAG ConnectThread: started.")
            device = this.ctDevice
            deviceUUID = this.uuid
        }

        override fun run() {
            Timber.d("$TAG RUN mConnectThread ")
            var tmp: BluetoothSocket? = null

            // Get a BluetoothSocket for a connection with a given BluetoothDevice
            try {
                Timber.d("$TAG ConnectThread: Trying to create InsecureRfcommSocket using UUID: $MY_UUID_INSECURE")
                tmp = device.createRfcommSocketToServiceRecord(deviceUUID)
            } catch (e: IOException) {
                Timber.e(e)
            }

            socket = tmp
            bluetoothAdapter.cancelDiscovery()

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a successful connection or an exception
                socket?.connect()

            } catch (e: IOException) {

                // close the socket
                try {
                    socket?.close()
                } catch (e1: IOException) {
                    Timber.d("$TAG mConnectThread: run: Unable to close connection in socket $e1")
                }

                Timber.e("$TAG run ConnectThread: Could not connect to UUID:  $MY_UUID_INSECURE")
            }

            // connected(socket, device)
        }

        private fun cancel() {
            try {
                Timber.d("$TAG cancel: Closing Client Socket.")
                socket?.close()
            } catch (e: IOException) {
                Timber.e("$TAG cancel: close() of socket in ConnectThread failed. $e")
            }
        }

        /**
         * Initiates the [AcceptThread] and listens for a connection
         *
         * Start the chat service. Specifically, start AcceptThread to begin a
         * session in listening (server) mode. Called by the Activity onResume()
         */
        @Synchronized override fun start() {
            Timber.d("$TAG start")

            // Cancel any thread attempting to make a connection
            connectThread?.cancel()
            connectThread = null

            if (insecureAcceptThread == null) {
                insecureAcceptThread = AcceptThread()
                insecureAcceptThread?.start() // native start method for the thread
            }
        }
    }

    inner class ConnectedThread(val socket: BluetoothSocket): Thread() {
        private lateinit var inputStream: InputStream
        private lateinit var outputStream: OutputStream

        init {
            Timber.d("$TAG ConnectedThread: Starting.")

            try {
                inputStream = socket.inputStream
                outputStream = socket.outputStream
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        override fun run() {
            val buffer = ByteArray(1024)          // buffer store for the stream

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                // Read from the InputStream
                try {
                    val bytes = inputStream.read(buffer)
                    val incomingMessage = String(buffer, 0, bytes)
                    Timber.d("$TAG InputStream: $incomingMessage")
                } catch (e: IOException) {
                    Timber.e("$TAG write: Error reading InputStream. $e")
                    break
                }
            }
        }

        // Call this from the main activity to send data to the remote device
        fun write(bytes: ByteArray) {
            val text = String(bytes, Charset.defaultCharset())
            Timber.d("$TAG write: Writing to OutputStream: $text")
            try {
                outputStream.write(bytes)
            } catch (e: IOException) {
                Timber.e("$TAG write: Error writing to OutputStream. $e")
            }

        }

        // Call this from the main activity to shutdown the connection
        fun cancel() {
            try {
                socket.close()
            } catch (e: IOException) { }
        }
    }

    /**
     * Initiates the [ConnectThread]
     *
     * [AcceptThread] starts and sits waiting for a connection.
     * Then [ConnectThread] starts and attempts to make a connection
     * with the other device's [AcceptThread]
     */
    fun startClient(device: BluetoothDevice, uuid: UUID) {
        Timber.d("$TAG startClient: Started.")

        ProgressDialog.show(context, "Connecting Bluetooth", "Please Wait...", true)

        connectThread = ConnectThread(device, uuid)
        connectThread?.start()
    }

    fun connected(bluetoothSocket: BluetoothSocket, bluetoothDevice: BluetoothDevice) {
        Timber.d("$TAG connected: Starting.")

        // Start the thread to manage the connection and perform transmissions
        connectedThread = ConnectedThread(bluetoothSocket)
        connectedThread?.start()
    }

    /**
     * Write to the [ConnectedThread] in an unsynchronized manner
     */
    fun write(out: ByteArray) {
        Timber.d("$TAG write: Write Called.")
        connectedThread?.write(out)
    }

    companion object {
        private val TAG = "BluetoothConnectionService"
        private val appName = "RESUMEBLU"
        private val MY_UUID_SECURE = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")
        private val MY_UUID_INSECURE = UUID.fromString("8ce255ce0-200a-11e0-ac64-0800200c9a66")
    }
}