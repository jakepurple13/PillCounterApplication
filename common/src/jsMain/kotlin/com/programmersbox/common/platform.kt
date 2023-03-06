package com.programmersbox.common

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

public actual fun getPlatformName(): String {
    return "JavaScript"
}

@Composable
public fun UIShow() {
    App()
}

internal actual class Database actual constructor(scope: CoroutineScope) {
    actual suspend fun list(): Flow<List<PillCount>> = flowOf(emptyList())
    actual suspend fun savePillWeightInfo(pillWeights: PillWeights) {}
    actual suspend fun removePillWeightInfo(pillWeights: PillWeights) {}
    actual suspend fun url(): Flow<String> = flowOf()
    actual suspend fun saveUrl(url: String) = Unit
    actual suspend fun updateInfo(pillCount: PillCount) = Unit
    actual suspend fun currentPill(): Flow<PillCount> = flowOf()
    actual suspend fun updateCurrentPill(pillCount: PillCount) = Unit
    actual suspend fun updateCurrentCountInfo(pillCount: PillCount) = Unit
    actual suspend fun urlHistory() = flowOf(emptyList<String>())
    actual suspend fun removeUrl(url: String) = Unit
}

internal actual fun DiscoveryViewModel.discover() {

}

internal actual fun randomUUID(): String = ""

internal actual val hasBLEDiscovery: Boolean = false

@Composable
public actual fun BluetoothDiscovery(viewModel: PillViewModel) {

}

@Composable
public fun <T, R> BluetoothScreen(
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
    actions: @Composable RowScope.() -> Unit = {}
) {
    BluetoothDiscoveryScreen(
        state,
        isConnecting,
        connectOverBle,
        device,
        deviceList,
        onDeviceClick,
        deviceIdentifier,
        deviceName,
        isDeviceSelected,
        networkItem,
        onNetworkItemClick,
        wifiNetworks,
        connectToWifi,
        getNetworks,
        ssid,
        signalStrength,
        actions
    )
}