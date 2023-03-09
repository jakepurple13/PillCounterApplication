package com.programmersbox.common

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programmersbox.database.PillWeightDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import moe.tlaster.precompose.PreComposeApplication
import moe.tlaster.precompose.ui.viewModel
import moe.tlaster.precompose.viewmodel.viewModelScope
import platform.Foundation.NSUUID
import platform.UIKit.UIViewController

public actual fun getPlatformName(): String {
    return "iOS"
}

@Composable
private fun UIShow(localization: Localization) {
    App(localization = localization)
}

internal var discoverAction: ((List<IpInfo>) -> Unit) -> Unit = {}

public fun MainViewController(
    actionOnDiscover: ((List<IpInfo>) -> Unit) -> Unit = {},
    localization: Localization
): UIViewController {
    discoverAction = actionOnDiscover
    return PreComposeApplication("PillCounter") {
        MaterialTheme(
            colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Spacer(Modifier.height(30.dp))
                    UIShow(localization)
                }
            }
        }
    }
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
    viewModelScope.launch {
        async {
            discoverAction { list ->
                discoveredList.addAll(list.map { PillCounterIp(it.host, it.name) }.distinct())
            }
        }.await()
        delay(30000)
        isSearching = false
    }
}

public class IpInfo(public val name: String, public val host: String)

internal actual fun randomUUID(): String = NSUUID.UUID().UUIDString()

internal actual val hasBLEDiscovery: Boolean = true

@Composable
internal actual fun BluetoothDiscovery(viewModel: PillViewModel) {
    val navigator = LocalNavigator.current
    val vm = viewModel(BluetoothViewModel::class) { BluetoothViewModel(navigator, viewModel) }
    BluetoothDiscoveryScreen(
        state = vm.state,
        isConnecting = vm.connecting,
        device = vm.advertisement,
        deviceList = vm.advertisementList,
        onDeviceClick = { it?.let(vm::click) },
        deviceIdentifier = { it?.identifier?.UUIDString.orEmpty() },
        deviceName = { it?.name ?: it?.peripheralName ?: "Device" },
        isDeviceSelected = { found, selected -> found?.identifier == selected?.identifier },
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
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = { ModalDrawerSheet { drawerContent() } },
        content = content
    )
}