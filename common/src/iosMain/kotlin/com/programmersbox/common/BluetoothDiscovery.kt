package com.programmersbox.common

import androidx.compose.runtime.*
import com.benasher44.uuid.uuidFrom
import com.juul.kable.*
import com.juul.kable.logs.Logging
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import moe.tlaster.precompose.navigation.Navigator
import moe.tlaster.precompose.viewmodel.ViewModel
import moe.tlaster.precompose.viewmodel.viewModelScope

internal class BluetoothViewModel(
    private val navigator: Navigator,
    private val viewModel: PillViewModel
) : ViewModel() {

    /*private val f by lazy {
        BlueFalcon(
            ApplicationContext(),
            BluetoothConstants.SERVICE_WIRELESS_SERVICE
        )
    }*/

    private val scanner by lazy {
        Scanner {
            filters = listOf(Filter.Service(uuidFrom(BluetoothConstants.SERVICE_WIRELESS_SERVICE)))
            logging {
                level = Logging.Level.Events
                data = Logging.DataProcessor { bytes ->
                    // todo: Convert `bytes` to desired String representation, for example:
                    bytes.joinToString { byte -> byte.toString() } // Show data as integer representation of bytes.
                }
            }
        }
    }

    /*private val b by lazy {
        BluetoothBuilder(
            permissionsBuilder = {
                Permissions(
                    PermissionsBuilder().apply {
                        registerBluetoothPermission()
                        registerLocationPermission()
                    }
                )
            },
        ).create()
    }*/

    val advertisementList = mutableStateListOf<Advertisement>()
    val wifiNetworks = mutableStateListOf<NetworkList>()
    var networkItem: NetworkList? by mutableStateOf(null)
    var state by mutableStateOf(BluetoothState.Searching)

    var advertisement: Advertisement? by mutableStateOf(null)
    var connecting by mutableStateOf(false)

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private var peripheral: Peripheral? = null

    init {
        //b

        /*f.scan()
        f.peripherals
            .collect {
                println("---")
                it.forEach {
                    println("---")
                    println(it.name)
                    println(it.uuid)
                }
            }*/

        scanner.advertisements
            .filter { it.identifier.UUIDString == BluetoothConstants.PI_MAC_ADDRESS }
            .onEach { a ->
                if (advertisementList.none { it.identifier.UUIDString == a.identifier.UUIDString })
                    advertisementList.add(a)
            }
            .launchIn(viewModelScope)
    }

    fun disconnect() {
        viewModelScope.cancel()
    }

    fun connect() {
        viewModelScope.launch {
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
                    BluetoothConstants.SERVICE_WIRELESS_SERVICE,
                    BluetoothConstants.CHARACTERISTIC_WIRELESS_COMMANDER_RESPONSE
                )
            )
                ?.onEach {
                    println("Kable content")
                    println(it)
                    if (refresh) networkString = ""
                    networkString += it.decodeToString()
                    refresh = 10 in it
                }
                ?.launchIn(viewModelScope)

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
                .launchIn(viewModelScope)

            peripheral?.state
                ?.onEach { println(it) }
                ?.launchIn(viewModelScope)

            sendScan()
            getNetworks()
        }
    }

    private fun sendScan() {
        viewModelScope.launch {
            peripheral?.write(
                characteristicOf(
                    BluetoothConstants.SERVICE_WIRELESS_SERVICE,
                    BluetoothConstants.CHARACTERISTIC_WIRELESS_COMMANDER
                ),
                BluetoothConstants.SCAN.encodeToByteArray(),
                WriteType.WithResponse
            )
        }
    }

    fun getNetworks() {
        viewModelScope.launch {
            peripheral?.write(
                characteristicOf(
                    BluetoothConstants.SERVICE_WIRELESS_SERVICE,
                    BluetoothConstants.CHARACTERISTIC_WIRELESS_COMMANDER
                ),
                BluetoothConstants.GET_NETWORKS.encodeToByteArray(),
                WriteType.WithResponse
            )
        }
    }

    fun click(advertisement: Advertisement) {
        this.advertisement = advertisement
        peripheral = viewModelScope.peripheral(advertisement)
    }

    fun networkClick(networkList: NetworkList?) {
        networkItem = networkList
    }

    fun connectToWifi(password: String) {
        peripheral?.let { p ->
            networkItem?.e?.let { ssid ->
                viewModelScope.launch {
                    (Json.encodeToString(ConnectRequest(c = 1, p = WiFiInfo(e = ssid, p = password))) + "\n")
                        .also { println(it) }
                        .chunked(20) { it.toString().encodeToByteArray() }
                        .onEach {
                            println(it.contentToString())
                            println(it.decodeToString())
                        }
                        .forEach {
                            p.write(
                                characteristicOf(
                                    BluetoothConstants.SERVICE_WIRELESS_SERVICE,
                                    BluetoothConstants.CHARACTERISTIC_WIRELESS_COMMANDER
                                ),
                                it,
                                WriteType.WithResponse
                            )
                        }
                    state = BluetoothState.Checking
                    delay(5000)
                    p.disconnect()
                    viewModel.reconnect()
                    navigator.goBack()
                    navigator.goBack()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}