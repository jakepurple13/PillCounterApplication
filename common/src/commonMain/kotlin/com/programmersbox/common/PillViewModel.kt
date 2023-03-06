package com.programmersbox.common

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import moe.tlaster.precompose.navigation.Navigator

public class PillViewModel(
    private val navigator: Navigator,
    private val viewModelScope: CoroutineScope
) {
    internal val db = Database(viewModelScope)

    internal var network: Network? = null

    public var pillCount: PillCount by mutableStateOf(PillCount(0.0, PillWeights()))
        private set

    public val pillWeightList: SnapshotStateList<PillCount> = mutableStateListOf()

    internal val pillAlreadySaved by derivedStateOf {
        pillWeightList.any { it.pillWeights.uuid == pillCount.pillWeights.uuid }
    }

    private var firstTime = true

    internal var connectedState by mutableStateOf(ConnectionState.Idle)

    internal val connectionError by derivedStateOf { connectedState == ConnectionState.Error }

    internal var showErrorBanner by mutableStateOf(false)

    internal val urlHistory = mutableStateListOf<String>()

    internal var url by mutableStateOf("")

    init {
        snapshotFlow { connectedState }
            .filter { connectedState == ConnectionState.Connected }
            .onEach {
                delay(2500)
                connectedState = ConnectionState.Idle
            }
            .launchIn(viewModelScope)

        snapshotFlow { connectedState }
            .onEach { showErrorBanner = connectionError }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            db.list()
                .onEach {
                    pillWeightList.clear()
                    pillWeightList.addAll(it)
                }
                .launchIn(this)
        }

        viewModelScope.launch {
            db.currentPill()
                .onEach { pillCount = it }
                .launchIn(this)
        }

        viewModelScope.launch {
            db.url()
                .filter { it.isNotEmpty() }
                .onEach { connectToNetwork(it) }
                .launchIn(this)
        }

        viewModelScope.launch {
            db.urlHistory()
                .onEach {
                    urlHistory.clear()
                    urlHistory.addAll(it)
                }
                .launchIn(this)
        }
    }

    internal fun removeUrl(url: String) {
        viewModelScope.launch { db.removeUrl(url) }
    }

    internal fun changeNetwork(url: String) {
        viewModelScope.launch { db.saveUrl(url) }
    }

    internal fun reconnect() {
        viewModelScope.launch {
            db.url().firstOrNull()?.let { connectToNetwork(it) }
        }
    }

    private suspend fun connectToNetwork(url: String) {
        network?.close()

        network = Network(Url("http://$url:8080"))
        this.url = url

        network!!.socketConnection()
            .catch {
                connectedState = ConnectionState.Error
                emit(Result.failure(it))
            }
            .onEach { result ->
                result
                    .onSuccess { pill ->
                        if (connectedState != ConnectionState.Idle || firstTime) {
                            firstTime = false
                            connectedState = ConnectionState.Connected
                        }
                        db.updateCurrentPill(pill)
                        if (pillWeightList.any { it.pillWeights.uuid == pill.pillWeights.uuid }) {
                            db.updateCurrentCountInfo(pill)
                        }
                    }
                    .onFailure { connectedState = ConnectionState.Error }
            }
            .launchIn(viewModelScope)
    }

    internal fun onDrawerItemMainScreenClick(pillWeights: PillWeights) {
        sendNewConfig(pillWeights)
    }

    public fun sendNewConfig(pillWeights: PillWeights) {
        viewModelScope.launch {
            network?.updateConfig(pillWeights)
                ?.onSuccess { println(it) }
                ?.onFailure { it.printStackTrace() }
        }
    }

    internal fun updateConfig(pillCount: PillCount) {
        viewModelScope.launch { db.updateInfo(pillCount) }
    }

    internal fun saveNewConfig(pillWeights: PillWeights) {
        viewModelScope.launch { db.savePillWeightInfo(pillWeights) }
    }

    internal fun removeConfig(pillWeights: PillWeights) {
        viewModelScope.launch { db.removePillWeightInfo(pillWeights) }
    }

    internal fun showNewPill() {
        navigator.navigateToNewPill()
    }

    public fun showMainScreen() {
        while (navigator.canGoBack) {
            navigator.goBack()
        }
    }

    internal fun showDiscovery() {
        navigator.navigateToDiscovery()
    }
}

public enum class PillState(internal val route: String = "") {
    MainScreen("home"),
    NewPill("new_pill"),
    Discovery("discovery"),
    BluetoothDiscovery("bluetooth")
}

internal enum class ConnectionState { Idle, Error, Connected }