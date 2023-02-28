import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.juul.kable.*
import com.juul.kable.logs.Logging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun <T, R> BluetoothDiscoveryScreen(
    state: BluetoothState,
    isConnecting: Boolean,
    connectOverBle: () -> Unit,
    device: T?,
    deviceList: List<T>,
    onDeviceClick: (T?) -> Unit,
    deviceIdentifier: (T?) -> String,
    deviceName: suspend (T?) -> String,
    isDeviceSelected: (found: T?, selected: T?) -> Boolean,
    networkItem: R?,
    onNetworkItemClick: (R?) -> Unit,
    wifiNetworks: List<R>,
    connectToWifi: (password: String) -> Unit,
    getNetworks: () -> Unit,
    ssid: (R?) -> String,
    signalStrength: (R?) -> Int,
    startScan: () -> Unit
) {
    AnimatedContent(
        state,
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
            BluetoothState.Searching -> BluetoothSearching(
                isConnecting = isConnecting,
                connectOverBle = connectOverBle,
                device = device,
                deviceList = deviceList,
                onDeviceClick = onDeviceClick,
                name = deviceName,
                isSelected = isDeviceSelected,
                identifier = deviceIdentifier,
                startScan = startScan
            )

            BluetoothState.Wifi -> WifiConnect(
                networkItem = networkItem,
                onNetworkItemClick = onNetworkItemClick,
                wifiNetworks = wifiNetworks,
                getNetworks = getNetworks,
                connectToWifi = connectToWifi,
                ssid = ssid,
                signalStrength = signalStrength
            )

            BluetoothState.Checking -> {
                Surface {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Text("Please Wait...")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> BluetoothSearching(
    device: T?,
    deviceList: List<T>,
    onDeviceClick: (T?) -> Unit,
    identifier: (T?) -> String,
    name: suspend (T?) -> String,
    isSelected: (found: T?, selected: T?) -> Boolean,
    isConnecting: Boolean,
    connectOverBle: () -> Unit,
    startScan: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Find PillCounter") },
                actions = {
                    IconButton(
                        onClick = startScan
                    ) { Icon(Icons.Default.Wifi, null) }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Button(
                    onClick = connectOverBle,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = device != null && !isConnecting
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
                items(deviceList) {
                    Card(onClick = { onDeviceClick(it) }) {
                        var deviceName by remember { mutableStateOf("") }
                        LaunchedEffect(Unit) { deviceName = name(it) }
                        ListItem(
                            headlineText = { Text(identifier(it)) },
                            supportingText = { Text(deviceName) },
                            trailingContent = {
                                if (isSelected(it, device)) {
                                    Icon(Icons.Default.Check, null)
                                }
                            }
                        )
                    }
                }
            }
            if (isConnecting) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <R> WifiConnect(
    networkItem: R?,
    onNetworkItemClick: (R?) -> Unit,
    wifiNetworks: List<R>,
    connectToWifi: (password: String) -> Unit,
    getNetworks: () -> Unit,
    ssid: (R?) -> String,
    signalStrength: (R?) -> Int
) {
    var password by remember { mutableStateOf("") }
    var hidePassword by remember { mutableStateOf(true) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connect PillCounter to Wifi") },
                actions = {
                    AnimatedVisibility(
                        networkItem != null
                    ) {
                        IconButton(
                            onClick = { onNetworkItemClick(null) }
                        ) { Icon(Icons.Default.Close, null) }
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        icon = { Icon(Icons.Default.Send, null) },
                        text = { Text("Connect") },
                        onClick = { connectToWifi(password) }
                    )
                },
                actions = {
                    Button(
                        onClick = getNetworks,
                    ) {
                        Icon(Icons.Default.Refresh, null)
                        Text("Refresh Networks")
                    }
                }
            )
        }
    ) { padding ->
        Crossfade(networkItem) { target ->
            Box(Modifier.padding(padding)) {
                if (target != null) {
                    Column(modifier = Modifier.align(Alignment.Center)) {
                        OutlinedTextField(
                            value = ssid(target),
                            onValueChange = {},
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            label = { Text("SSID") },
                            leadingIcon = { Icon(Icons.Default.Wifi, null) },
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            singleLine = true,
                            visualTransformation = if (hidePassword) PasswordVisualTransformation() else VisualTransformation.None,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.WifiPassword, null) },
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
                        items(wifiNetworks) {
                            Card(onClick = { onNetworkItemClick(it) }) {
                                ListItem(
                                    headlineText = { Text(ssid(it)) },
                                    leadingContent = {
                                        Icon(
                                            when (signalStrength(it)) {
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

    /*init {
        scanner.advertisements
            .onEach { a ->
                if (advertisementList.none { it.peripheralName == a.peripheralName }) advertisementList.add(a)
            }
            .launchIn(scannerScope)
    }*/

    fun startScan() {
        scanner.advertisements
            .onEach { a ->
                advertisementList.add(a)
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
                SCAN.encodeToByteArray(),
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
                GET_NETWORKS.encodeToByteArray(),
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


@Serializable
internal data class ConnectRequest(
    val c: Int,
    val p: WiFiInfo
)

@Serializable
internal data class SingleCommand(val c: Int)

@Serializable
internal data class WiFiInfo(
    val e: String,
    val p: String
)

@Serializable
internal data class WifiList(
    var c: Int,
    var r: Int,
    var p: List<NetworkList> = emptyList()
)

@Serializable
public data class NetworkList(
    var e: String? = null,
    var m: String? = null,
    var s: Int? = null,
    var p: Int? = null
)

public enum class BluetoothState {
    Searching, Wifi, Checking
}