package com.example.leonardocontroller.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

class BluetoothManager(
    private val context: Context
) {

    private val bluetoothAdapter: BluetoothAdapter? =
        BluetoothAdapter.getDefaultAdapter()

    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    private val sppUuid: UUID =
        UUID.fromString(
            "00001101-0000-1000-8000-00805F9B34FB"
        )


    fun isBluetoothSupported(): Boolean {
        return bluetoothAdapter != null
    }


    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }


    @SuppressLint("MissingPermission")
    fun isConnected(): Boolean {
        return bluetoothSocket?.isConnected == true
    }


    @SuppressLint("MissingPermission")
    suspend fun connectToHC05(): Result<String> {

        return withContext(Dispatchers.IO) {

            try {

                if (bluetoothAdapter == null) {
                    return@withContext Result.failure(
                        Exception("Bluetooth is not supported")
                    )
                }

                if (!bluetoothAdapter.isEnabled) {
                    return@withContext Result.failure(
                        Exception("Bluetooth is turned OFF")
                    )
                }

                /*
                 * HC-05 must already be paired with the phone.
                 */

                val pairedDevices =
                    bluetoothAdapter.bondedDevices

                val hc05: BluetoothDevice? =
                    pairedDevices.firstOrNull {

                        it.name.equals(
                            "HC-05",
                            ignoreCase = true
                        )
                    }

                if (hc05 == null) {

                    return@withContext Result.failure(
                        Exception(
                            "HC-05 is not paired with this phone"
                        )
                    )
                }

                /*
                 * Close any previous connection.
                 */

                try {
                    bluetoothSocket?.close()
                } catch (_: Exception) {
                }

                bluetoothSocket = null
                outputStream = null


                /*
                 * Stop discovery before connecting.
                 */

                try {
                    bluetoothAdapter.cancelDiscovery()
                } catch (_: Exception) {
                }


                /*
                 * Create classic Bluetooth SPP socket.
                 */

                val socket =
                    hc05.createRfcommSocketToServiceRecord(
                        sppUuid
                    )

                bluetoothSocket = socket


                /*
                 * Connect to HC-05.
                 */

                socket.connect()


                outputStream =
                    socket.outputStream


                Result.success(
                    "Connected to ${hc05.name}"
                )

            } catch (e: Exception) {

                try {
                    bluetoothSocket?.close()
                } catch (_: Exception) {
                }

                bluetoothSocket = null
                outputStream = null

                Result.failure(
                    Exception(
                        e.message ?: "Bluetooth connection failed"
                    )
                )
            }
        }
    }


    suspend fun sendCommand(
        command: Char
    ): Result<Unit> {

        return withContext(Dispatchers.IO) {

            try {

                if (!isConnected()) {

                    return@withContext Result.failure(
                        Exception(
                            "HC-05 is not connected"
                        )
                    )
                }

                val stream =
                    outputStream
                        ?: return@withContext Result.failure(
                            Exception(
                                "Bluetooth output stream unavailable"
                            )
                        )

                stream.write(
                    command.code
                )

                stream.flush()

                Result.success(Unit)

            } catch (e: IOException) {

                Result.failure(
                    Exception(
                        e.message
                            ?: "Failed to send command"
                    )
                )
            }
        }
    }


    fun disconnect() {

        try {
            outputStream?.close()
        } catch (_: Exception) {
        }

        try {
            bluetoothSocket?.close()
        } catch (_: Exception) {
        }

        outputStream = null
        bluetoothSocket = null
    }
}