import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.skiko.wasm.onWasmReady

fun main() {
    onWasmReady {
        BrowserViewportWindow("PillCounter") {
            MaterialTheme(darkColorScheme()) {
                Surface(Modifier.fillMaxSize()) {
                    var showIt by remember { mutableStateOf(js("navigator.bluetooth.requestDevice") != null) }
                    val vm = remember { BluetoothViewModel() }
                    if (showIt) {
                        BluetoothDiscoveryScreen(
                            state = vm.state,
                            isConnecting = vm.connecting,
                            device = vm.advertisement,
                            deviceList = vm.advertisementList,
                            onDeviceClick = { it?.let(vm::click) },
                            deviceIdentifier = { it?.name.orEmpty() },
                            deviceName = { it?.name ?: it?.peripheralName ?: "Device" },
                            isDeviceSelected = { found, selected -> found == selected },
                            networkItem = vm.networkItem,
                            onNetworkItemClick = { vm.networkClick(it) },
                            wifiNetworks = vm.wifiNetworks,
                            connectToWifi = vm::connectToWifi,
                            getNetworks = vm::getNetworks,
                            ssid = { it?.e ?: "No SSID" },
                            signalStrength = { it?.s ?: 0 },
                            connectOverBle = vm::connect,
                            startScan = vm::startScan
                        )
                    } else {
                        Button(
                            onClick = {
                                vm.startScan()
                                val f = js("navigator.bluetooth.requestDevice")
                                showIt = f != null
                            }
                        ) { Text("Permission") }
                    }
                }
            }
        }
    }
}