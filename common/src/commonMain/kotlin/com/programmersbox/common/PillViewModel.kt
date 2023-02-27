package com.programmersbox.common

import androidx.compose.runtime.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import moe.tlaster.precompose.navigation.Navigator

public class PillViewModel(
    private val navigator: Navigator,
    private val viewModelScope: CoroutineScope
) {
    internal val db = Database(viewModelScope)

    internal var network: Network? = null
    internal var isConnectionLoading by mutableStateOf(false)

    public var pillCount: PillCount by mutableStateOf(PillCount(0.0, PillWeights()))
        private set

    internal val pillWeightList = mutableStateListOf<PillCount>()

    internal var connectionError by mutableStateOf(false)

    internal val pillAlreadySaved by derivedStateOf {
        pillWeightList.any { it.pillWeights.uuid == pillCount.pillWeights.uuid }
    }

    init {
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
    }

    internal fun changeNetwork(url: String) {
        viewModelScope.launch { db.saveUrl(url) }
    }

    internal fun reconnect() {
        viewModelScope.launch {
            if (!isConnectionLoading) db.url().firstOrNull()?.let { connectToNetwork(it) }
        }
    }

    private fun connectToNetwork(url: String) {
        isConnectionLoading = true

        network?.close()

        network = Network(Url("http://$url:8080"))

        network!!.socketConnection()
            .catch {
                it.printStackTrace()
                connectionError = true
                emit(Result.failure(it))
            }
            .onEach { result ->
                isConnectionLoading = false
                result
                    .onSuccess { pill ->
                        connectionError = false
                        db.updateCurrentPill(pill)
                        if (pillWeightList.any { it.pillWeights.uuid == pill.pillWeights.uuid }) {
                            db.updateCurrentCountInfo(pill)
                        }
                    }
                    .onFailure {
                        it.printStackTrace()
                        connectionError = true
                    }
            }
            .launchIn(viewModelScope)
    }

    internal fun onDrawerItemMainScreenClick(pillWeights: PillWeights) {
        sendNewConfig(pillWeights)
    }

    private fun sendNewConfig(pillWeights: PillWeights) {
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