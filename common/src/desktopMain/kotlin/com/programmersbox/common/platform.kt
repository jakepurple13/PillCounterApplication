package com.programmersbox.common

import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.runtime.Composable
import com.programmersbox.database.PillWeightDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import moe.tlaster.precompose.navigation.Navigator
import moe.tlaster.precompose.ui.viewModel
import moe.tlaster.precompose.viewmodel.viewModelScope
import java.net.InetAddress
import java.util.*
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceListener

public actual fun getPlatformName(): String {
    return "Desktop"
}

@Composable
public fun UIShow(
    localization: Localization,
    navigator: Navigator,
    vm: PillViewModel,
) {
    App(
        localization = localization,
        navigator = navigator,
        vm = vm
    )
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
        val jmdns = JmDNS.create(InetAddress.getLocalHost(), "HOST")
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

internal actual val hasBLEDiscovery: Boolean = false

@Composable
internal actual fun BluetoothDiscovery(viewModel: PillViewModel) {
    val navigator = LocalNavigator.current
    val vm = viewModel { BluetoothViewModel(navigator, viewModel) }
    BluetoothDiscoveryScreen(
        state = vm.state,
        isConnecting = vm.connecting,
        device = vm.device,
        deviceList = vm.deviceList,
        onDeviceClick = { it?.let(vm::click) },
        deviceIdentifier = { it ?: "" },
        deviceName = { it ?: "Device" },
        isDeviceSelected = { found, selected -> found == selected },
        networkItem = vm.networkItem,
        onNetworkItemClick = vm::networkClick,
        wifiNetworks = vm.wifiNetworks,
        connectToWifi = vm::connectToWifi,
        getNetworks = vm::getNetworks,
        ssid = { it?.e.orEmpty() },
        signalStrength = { it?.s ?: 0 },
        connectOverBle = vm::connect
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal actual fun DrawerType(
    drawerState: DrawerState,
    drawerContent: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    PermanentNavigationDrawer(
        drawerContent = { PermanentDrawerSheet { drawerContent() } },
        content = content
    )
}