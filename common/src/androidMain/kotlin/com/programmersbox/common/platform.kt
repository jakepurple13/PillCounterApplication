package com.programmersbox.common

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.programmersbox.database.PillWeightDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import moe.tlaster.precompose.ui.viewModel
import moe.tlaster.precompose.viewmodel.viewModelScope
import java.net.InetAddress
import java.util.*
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceListener

public actual fun getPlatformName(): String {
    return "Android"
}

@Composable
public fun UIShow(localization: Localization = Localization()) {
    App(localization = localization)
}

internal actual class Database actual constructor(scope: CoroutineScope) {
    private val db = PillWeightDatabase()
    actual suspend fun list(): Flow<List<PillCount>> = db.getItems()
        .mapNotNull { l ->
            l.map {
                PillCount(
                    count = it.currentCount,
                    PillWeights(
                        name = it.name,
                        pillWeight = it.pillWeight,
                        bottleWeight = it.bottleWeight,
                        uuid = it.uuid
                    )
                )
            }
        }

    actual suspend fun savePillWeightInfo(pillWeights: PillWeights) {
        db.saveInfo(
            name = pillWeights.name,
            pillWeight = pillWeights.pillWeight,
            bottleWeight = pillWeights.bottleWeight,
            uuid = pillWeights.uuid
        )
    }

    actual suspend fun removePillWeightInfo(pillWeights: PillWeights) {
        db.removeInfo(pillWeights.uuid)
    }

    actual suspend fun url(): Flow<String> = db.getUrl()
    actual suspend fun saveUrl(url: String) = db.saveUrl(url)
    actual suspend fun updateInfo(pillCount: PillCount) {
        db.updateInfo(pillCount.pillWeights.uuid) {
            currentCount = pillCount.count
            pillWeight = pillCount.pillWeights.pillWeight
            bottleWeight = pillCount.pillWeights.bottleWeight
            name = pillCount.pillWeights.name
        }
    }

    actual suspend fun updateCurrentCountInfo(pillCount: PillCount) {
        db.updateInfo(pillCount.pillWeights.uuid) {
            currentCount = pillCount.count
        }
    }

    actual suspend fun currentPill(): Flow<PillCount> = db.getLatest()
        .map {
            PillCount(
                count = it.currentCount,
                PillWeights(
                    name = it.name,
                    pillWeight = it.pillWeight,
                    bottleWeight = it.bottleWeight,
                    uuid = it.uuid
                )
            )
        }

    actual suspend fun updateCurrentPill(pillCount: PillCount) {
        db.updateLatest(
            currentCount = pillCount.count,
            name = pillCount.pillWeights.name,
            bottleWeight = pillCount.pillWeights.bottleWeight,
            pillWeight = pillCount.pillWeights.pillWeight,
            uuid = pillCount.pillWeights.uuid,
        )
    }

    actual suspend fun urlHistory() = db.getUrlHistory()
    actual suspend fun removeUrl(url: String) = db.removeUrl(url)
}

internal actual fun DiscoveryViewModel.discover() {
    isSearching = true
    viewModelScope.launch(Dispatchers.IO) {
        val jmdns = JmDNS.create(InetAddress.getByName("10.0.0.2"), "HOST")
        jmdns.addServiceListener(
            "_http._tcp.local.",
            object : ServiceListener {
                override fun serviceAdded(event: ServiceEvent) {
                    //println("Service added: " + event.info)
                }

                override fun serviceRemoved(event: ServiceEvent) {
                    //println("Service removed: " + event.info)
                }

                override fun serviceResolved(event: ServiceEvent) {
                    //println("Service resolved: " + event.info.inet4Addresses)
                    //println("Service resolved: " + event.info.inet4Addresses.map { it.hostName })
                    //println("Service resolved: " + event.info.inet4Addresses.map { it.hostAddress })
                    //val pillDevices = event.info.inet4Addresses.filter { it.canonicalHostName }
                    //println(pillDevices)
                    discoveredList.addAll(
                        event.info.inet4Addresses.mapNotNull {
                            it.hostAddress?.let { it1 -> PillCounterIp(it1, it.hostName) }
                        }
                    )
                }
            }
        )
        delay(30000)
        jmdns.unregisterAllServices()
        isSearching = false
    }
}

internal actual fun randomUUID(): String = UUID.randomUUID().toString()

internal actual val hasBLEDiscovery: Boolean = true

@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal actual fun BluetoothDiscovery(viewModel: PillViewModel) {
    val navigator = LocalNavigator.current
    PermissionRequest(
        listOf(
            *if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                listOf(
                    android.Manifest.permission.BLUETOOTH_SCAN,
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                )
            } else {
                listOf(
                    android.Manifest.permission.BLUETOOTH,
                    android.Manifest.permission.BLUETOOTH_ADMIN,
                )
            }.toTypedArray(),
            *if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                listOf(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                listOf(
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                )
            }.toTypedArray(),
        )
    ) {
        val vm = viewModel { BluetoothViewModel(navigator, viewModel) }
        BluetoothDiscoveryScreen(
            state = vm.state,
            isConnecting = vm.connecting,
            device = vm.advertisement,
            deviceList = vm.advertisementList,
            onDeviceClick = { it?.let(vm::click) },
            deviceIdentifier = { it?.address.orEmpty() },
            deviceName = { it?.name ?: it?.peripheralName ?: "Device" },
            isDeviceSelected = { found, selected -> found?.address == selected?.address },
            networkItem = vm.networkItem,
            onNetworkItemClick = vm::networkClick,
            wifiNetworks = vm.wifiNetworks,
            connectToWifi = vm::connectToWifi,
            getNetworks = vm::getNetworks,
            ssid = { it?.e ?: "No SSID" },
            signalStrength = { it?.s ?: 0 },
            connectOverBle = vm::connect
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalPermissionsApi
@Composable
internal fun PermissionRequest(permissionsList: List<String>, content: @Composable () -> Unit) {
    val permissions = rememberMultiplePermissionsState(permissionsList)
    val context = LocalContext.current
    SideEffect { permissions.launchMultiplePermissionRequest() }
    if (permissions.allPermissionsGranted) {
        content()
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("PillCounter WiFi Setup") },
                    navigationIcon = { BackButton() }
                )
            }
        ) { padding ->
            if (permissions.shouldShowRationale) {
                NeedsPermissions(padding) { permissions.launchMultiplePermissionRequest() }
            } else {
                NeedsPermissions(padding) {
                    context.startActivity(
                        Intent().apply {
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    )
                }
            }
        }
    }
}

@Composable
internal fun NeedsPermissions(paddingValues: PaddingValues, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 30.dp)
            .padding(paddingValues),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Please Enable Bluetooth/Nearby Devices and Location Permissions",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )

        Text(
            text = "They are needed to connect to a PillCounter device",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Button(
            onClick = onClick,
            modifier = Modifier.padding(bottom = 4.dp)
        ) { Text(text = "Enable") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal actual fun DrawerType(
    drawerState: DrawerState,
    drawerContent: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = { ModalDrawerSheet { drawerContent() } },
        content = content
    )
}