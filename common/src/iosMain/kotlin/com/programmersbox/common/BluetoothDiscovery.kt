package com.programmersbox.common

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.benasher44.uuid.uuidFrom
import com.juul.kable.*
import com.juul.kable.logs.Logging
import com.splendo.kaluga.bluetooth.BluetoothBuilder
import com.splendo.kaluga.permissions.base.Permissions
import com.splendo.kaluga.permissions.base.PermissionsBuilder
import com.splendo.kaluga.permissions.bluetooth.registerBluetoothPermission
import com.splendo.kaluga.permissions.location.registerLocationPermission
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun BluetoothDiscoveryScreen(viewModel: PillViewModel) {
    val vm = remember { BluetoothViewModel(viewModel) }
    AnimatedContent(
        vm.state,
        transitionSpec = {
            if (targetState > initialState) {
                slideInHorizontally { width -> width } + fadeIn() with
                        slideOutHorizontally { width -> -width } + fadeOut()
            } else {
                slideInHorizontally { width -> -width } + fadeIn() with
                        slideOutHorizontally { width -> width } + fadeOut()
            }.using(SizeTransform(clip = false))
        }
    ) { target ->
        when (target) {
            BluetoothState.Searching -> BluetoothSearching(vm)
            BluetoothState.Wifi -> WifiConnect(vm)
            BluetoothState.Checking -> TODO()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BluetoothSearching(vm: BluetoothViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Find PillCounter") }
            )
        },
        bottomBar = {
            BottomAppBar {
                Button(
                    onClick = { vm.connect() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = vm.advertisement != null && !vm.connecting
                ) { Text("Connect") }
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn {
                items(vm.advertisementList) {
                    Card(onClick = { vm.click(it) }) {
                        ListItem(
                            headlineText = { Text(it.identifier.UUIDString) },
                            supportingText = { Text(it.name ?: it.peripheralName ?: "Device") },
                            trailingContent = {
                                if (vm.advertisement?.identifier?.UUIDString == it.identifier.UUIDString) {
                                    Icon(Icons.Default.Check, null)
                                }
                            }
                        )
                    }
                }
            }
            if (vm.connecting) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WifiConnect(vm: BluetoothViewModel) {
    var password by remember { mutableStateOf("") }
    var hidePassword by remember { mutableStateOf(true) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connect PillCounter to Wifi") }
            )
        },
        bottomBar = {
            BottomAppBar {
                Button(
                    onClick = { vm.connectToWifi(password) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Connect") }
            }
        }
    ) { padding ->
        Crossfade(vm.networkItem) { target ->
            Box(Modifier.padding(padding)) {
                if (target != null) {
                    Column(modifier = Modifier.align(Alignment.Center)) {
                        OutlinedTextField(
                            value = target.e.orEmpty(),
                            onValueChange = {},
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            label = { Text("SSID") },
                            leadingIcon = {
                                IconButton(
                                    onClick = { vm.networkClick(null) }
                                ) { Icon(Icons.Default.Close, null) }
                            }
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            visualTransformation = if (hidePassword) PasswordVisualTransformation() else VisualTransformation.None,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Password") },
                            trailingIcon = {
                                IconToggleButton(
                                    hidePassword,
                                    onCheckedChange = { hidePassword = it }
                                ) {
                                    Icon(
                                        if (hidePassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        null
                                    )
                                }
                            }
                        )
                    }
                } else {
                    LazyColumn {
                        items(vm.wifiNetworks) {
                            Card(onClick = { vm.networkClick(it) }) {
                                ListItem(
                                    headlineText = { Text(it.e.toString()) },
                                    leadingContent = {
                                        Icon(
                                            when (it.s) {
                                                in 0..25 -> Icons.Default.NetworkWifi1Bar
                                                in 25..50 -> Icons.Default.NetworkWifi2Bar
                                                in 50..75 -> Icons.Default.NetworkWifi3Bar
                                                in 50..75 -> Icons.Default.SignalWifi4Bar
                                                else -> Icons.Default.SignalWifiStatusbarNull
                                            },
                                            null
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

internal enum class BluetoothState {
    Searching, Wifi, Checking
}

internal class BluetoothViewModel(private val viewModel: PillViewModel) {

    private val scannerScope = CoroutineScope(Dispatchers.Main + Job())

    private val scanner by lazy {
        Scanner {
            filters = listOf(Filter.Service(uuidFrom(SERVICE_WIRELESS_SERVICE)))
            logging {
                level = Logging.Level.Events
                data = Logging.DataProcessor { bytes ->
                    // todo: Convert `bytes` to desired String representation, for example:
                    bytes.joinToString { byte -> byte.toString() } // Show data as integer representation of bytes.
                }
            }
        }
    }

    private val b by lazy {
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
    }

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
        b

        scanner.advertisements
            .filter { it.identifier.UUIDString == PI_MAC_ADDRESS }
            .onEach { a ->
                if (advertisementList.none { it.identifier.UUIDString == a.identifier.UUIDString })
                    advertisementList.add(a)
            }
            .launchIn(scannerScope)
    }

    fun disconnect() {
        scannerScope.cancel()
    }

    fun connect() {
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
            getNetworksK()
        }
    }

    private fun sendScan() {
        scannerScope.launch {
            peripheral?.write(
                characteristicOf(
                    SERVICE_WIRELESS_SERVICE,
                    CHARACTERISTIC_WIRELESS_COMMANDER
                ),
                SCAN.encodeToByteArray(),
                WriteType.WithResponse
            )
        }
    }

    private fun getNetworksK() {
        scannerScope.launch {
            peripheral?.write(
                characteristicOf(
                    SERVICE_WIRELESS_SERVICE,
                    CHARACTERISTIC_WIRELESS_COMMANDER
                ),
                GET_NETWORKS.encodeToByteArray(),
                WriteType.WithResponse
            )
        }
    }

    fun click(advertisement: Advertisement) {
        this.advertisement = advertisement
        peripheral = scannerScope.peripheral(advertisement)
    }

    fun networkClick(networkList: NetworkList?) {
        networkItem = networkList
    }

    fun connectToWifi(password: String) {
        peripheral?.let { p ->
            networkItem?.e?.let { ssid ->
                scannerScope.launch {
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
                                    SERVICE_WIRELESS_SERVICE,
                                    CHARACTERISTIC_WIRELESS_COMMANDER
                                ),
                                it,
                                WriteType.WithResponse
                            )
                        }
                    p.disconnect()
                    viewModel.showMainScreen()
                }
            }
        }
    }

    companion object {
        const val SERVICE_WIRELESS_SERVICE = "e081fec0-f757-4449-b9c9-bfa83133f7fc"
        const val CHARACTERISTIC_WIRELESS_COMMANDER = "e081fec1-f757-4449-b9c9-bfa83133f7fc"
        const val CHARACTERISTIC_WIRELESS_COMMANDER_RESPONSE = "e081fec2-f757-4449-b9c9-bfa83133f7fc"
        const val CHARACTERISTIC_WIRELESS_CONNECTION_STATUS = "e081fec3-f757-4449-b9c9-bfa83133f7fc"

        private const val GET_NETWORKS = "{\"c\":0}\n"
        private const val SCAN = "{\"c\":4}\n"
        private const val PI_MAC_ADDRESS = "B8:27:EB:E2:8F:2F"
    }
}