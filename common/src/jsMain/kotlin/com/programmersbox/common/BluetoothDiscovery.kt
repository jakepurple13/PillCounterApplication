package com.programmersbox.common

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.juul.kable.*
import com.juul.kable.logs.Logging
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

public class BluetoothViewModel {

    private val scannerScope = CoroutineScope(Dispatchers.Main + Job())

    private val scanner by lazy {
        Scanner {
            filters = null//listOf(Filter.Service(uuidFrom(SERVICE_WIRELESS_SERVICE)))
            logging {
                level = Logging.Level.Events
                data = Logging.DataProcessor { bytes ->
                    bytes.joinToString { byte -> byte.toString() } // Show data as integer representation of bytes.
                }
            }
        }
    }

    public val advertisementList: SnapshotStateList<Advertisement> = mutableStateListOf<Advertisement>()
    public val wifiNetworks: SnapshotStateList<NetworkList> = mutableStateListOf<NetworkList>()
    public var networkItem: NetworkList? by mutableStateOf(null)
    public var state: BluetoothState by mutableStateOf(BluetoothState.Searching)

    public var advertisement: Advertisement? by mutableStateOf(null)
    public var connecting: Boolean by mutableStateOf(false)

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private var peripheral: Peripheral? = null

    public fun startScan() {
        scanner.advertisements
            .onEach { a ->
                if (advertisementList.none { it.peripheralName == a.peripheralName }) advertisementList.add(a)
            }
            .launchIn(scannerScope)
    }

    private fun disconnect() {
        scannerScope.cancel()
    }

    public fun connect() {
        scannerScope.launch {
            connecting = true
            try {
                peripheral?.connect()
            } catch (e: Exception) {
                e.printStackTrace()
                return@launch
            } finally {
                connecting = false
            }
            state = BluetoothState.Wifi

            var networkString by mutableStateOf("")
            var refresh = false

            peripheral?.observe(
                characteristicOf(
                    SERVICE_WIRELESS_SERVICE,
                    CHARACTERISTIC_WIRELESS_COMMANDER_RESPONSE
                )
            )
                ?.onEach {
                    println("Kable content")
                    println(it)
                    if (refresh) networkString = ""
                    networkString += it.decodeToString()
                    refresh = 10 in it
                }
                ?.launchIn(scannerScope)

            snapshotFlow { networkString }
                .onEach { println(it) }
                .filter { it.isNotEmpty() }
                .filter { "\n" in it }
                .onEach {
                    println("Command!")
                    val value = it.removeSuffix("\n")
                    val command = json.decodeFromString<SingleCommand>(value)
                    println(command)
                    when (command.c) {
                        0 -> {
                            wifiNetworks.clear()
                            wifiNetworks.addAll(json.decodeFromString<WifiList>(value).p)
                        }

                        1 -> {

                        }

                        2 -> {

                        }

                        4 -> {

                        }
                    }
                }
                .launchIn(scannerScope)

            peripheral?.state
                ?.onEach { println(it) }
                ?.launchIn(scannerScope)

            sendScan()
            getNetworks()
        }
    }

    private fun sendScan() {
        scannerScope.launch {
            peripheral?.write(
                characteristicOf(
                    SERVICE_WIRELESS_SERVICE,
                    CHARACTERISTIC_WIRELESS_COMMANDER
                ),
                SCAN.toByteArray(),
                WriteType.WithResponse
            )
        }
    }

    public fun getNetworks() {
        scannerScope.launch {
            peripheral?.write(
                characteristicOf(
                    SERVICE_WIRELESS_SERVICE,
                    CHARACTERISTIC_WIRELESS_COMMANDER
                ),
                GET_NETWORKS.toByteArray(),
                WriteType.WithResponse
            )
        }
    }

    public fun click(advertisement: Advertisement) {
        this.advertisement = advertisement
        peripheral = scannerScope.peripheral(advertisement)
    }

    public fun networkClick(networkList: NetworkList?) {
        networkItem = networkList
    }

    public fun connectToWifi(password: String) {
        peripheral?.let { p ->
            networkItem?.e?.let { ssid ->
                scannerScope.launch {
                    (Json.encodeToString(ConnectRequest(c = 1, p = WiFiInfo(e = ssid, p = password))) + "\n")
                        .also { println(it) }
                        .chunked(20) { it.toString().toByteArray() }
                        .onEach {
                            println(it.contentToString())
                            println(it.decodeToString())
                        }
                        .forEach {
                            p.write(
                                characteristicOf(
                                    SERVICE_WIRELESS_SERVICE,
                                    CHARACTERISTIC_WIRELESS_COMMANDER
                                ),
                                it,
                                WriteType.WithResponse
                            )
                        }
                    state = BluetoothState.Checking
                    delay(5000)
                    p.disconnect()
                    //viewModel.reconnect()
                    //navigator.goBack()
                    //navigator.goBack()
                }
            }
        }
    }

    private companion object {
        const val SERVICE_WIRELESS_SERVICE = "e081fec0-f757-4449-b9c9-bfa83133f7fc"
        const val CHARACTERISTIC_WIRELESS_COMMANDER = "e081fec1-f757-4449-b9c9-bfa83133f7fc"
        const val CHARACTERISTIC_WIRELESS_COMMANDER_RESPONSE = "e081fec2-f757-4449-b9c9-bfa83133f7fc"
        const val CHARACTERISTIC_WIRELESS_CONNECTION_STATUS = "e081fec3-f757-4449-b9c9-bfa83133f7fc"

        private const val GET_NETWORKS = "{\"c\":0}\n"
        private const val SCAN = "{\"c\":4}\n"
        private const val PI_MAC_ADDRESS = "B8:27:EB:E2:8F:2F"
    }
}
