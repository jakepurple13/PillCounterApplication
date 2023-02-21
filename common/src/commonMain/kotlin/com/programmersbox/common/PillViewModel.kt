package com.programmersbox.common

import androidx.compose.runtime.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

public class PillViewModel(private val scope: CoroutineScope) {

    private val db = Database(scope)

    private var network: Network? = null

    public var pillCount: PillCount by mutableStateOf(PillCount(0.0, PillWeights()))
        private set

    internal val pillWeightList = mutableStateListOf<PillCount>()

    public var pillState: PillState by mutableStateOf(PillState.MainScreen)
    private var previousPillState: PillState = PillState.MainScreen

    internal var newPill by mutableStateOf(PillWeights())

    internal var isNewPillLoading by mutableStateOf(false)

    internal val pillAlreadySaved by derivedStateOf {
        pillWeightList.any { it.pillWeights.uuid == pillCount.pillWeights.uuid }
    }

    init {
        scope.launch {
            db.list()
                .onEach {
                    pillWeightList.clear()
                    pillWeightList.addAll(it)
                }
                .launchIn(this)
        }

        scope.launch {
            db.currentPill()
                .onEach { pillCount = it }
                .launchIn(this)
        }

        scope.launch {
            db.url()
                .filter { it.isNotEmpty() }
                .onEach { connectToNetwork(it) }
                .launchIn(this)
        }
    }

    internal fun changeNetwork(url: String) {
        scope.launch { db.saveUrl(url) }
    }

    internal fun reconnect() {
        scope.launch { db.url().firstOrNull()?.let { connectToNetwork(it) } }
    }

    private fun connectToNetwork(url: String) {
        network?.close()

        network = Network(Url("http://$url:8080"))

        network!!.socketConnection()
            .catch {
                it.printStackTrace()
                emit(Result.failure(it))
            }
            .onEach { result ->
                result
                    .onSuccess { pill ->
                        pillState = PillState.MainScreen
                        db.updateCurrentPill(pill)
                        if (pillWeightList.any { it.pillWeights.uuid == pill.pillWeights.uuid }) {
                            db.updateCurrentCountInfo(pill)
                        }
                    }
                    .onFailure { pillState = PillState.Error }
            }
            .launchIn(scope)
    }

    internal fun onDrawerItemClick(pillWeights: PillWeights) {
        when (pillState) {
            PillState.MainScreen -> sendNewConfig(pillWeights)
            PillState.NewPill -> recalibrate(pillWeights)
            else -> {}
        }
    }

    private fun recalibrate(pillWeights: PillWeights) {
        newPill = pillWeights
    }

    internal fun updatePill(
        name: String = newPill.name,
        pillWeight: Double = newPill.pillWeight,
        bottleWeight: Double = newPill.bottleWeight
    ) {
        newPill = newPill.copy(name = name, bottleWeight = bottleWeight, pillWeight = pillWeight)
    }

    internal fun calibratePillWeight() {
        network?.pillWeightCalibration()
            ?.onStart { isNewPillLoading = true }
            ?.onEach { updatePill(pillWeight = it.pillWeight) }
            ?.onCompletion { isNewPillLoading = false }
            ?.catch { it.printStackTrace() }
            ?.launchIn(scope)
    }

    internal fun calibrateBottleWeight() {
        network?.pillWeightCalibration()
            ?.onStart { isNewPillLoading = true }
            ?.onEach { updatePill(bottleWeight = it.bottleWeight) }
            ?.onCompletion { isNewPillLoading = false }
            ?.catch { it.printStackTrace() }
            ?.launchIn(scope)
    }

    internal fun sendNewConfig(pillWeights: PillWeights) {
        scope.launch {
            network?.updateConfig(pillWeights)
                ?.onSuccess { println(it) }
                ?.onFailure { it.printStackTrace() }
        }
    }

    internal fun updateConfig(pillCount: PillCount) {
        scope.launch { db.updateInfo(pillCount) }
    }

    internal fun saveNewConfig(pillWeights: PillWeights) {
        scope.launch { db.savePillWeightInfo(pillWeights) }
    }

    internal fun removeConfig(pillWeights: PillWeights) {
        scope.launch { db.removePillWeightInfo(pillWeights) }
    }

    internal fun showNewPill() {
        if (previousPillState == PillState.Error) {
            pillState = PillState.Error
        } else {
            previousPillState = pillState
            pillState = PillState.NewPill
        }
    }

    public fun showMainScreen() {
        if (previousPillState == PillState.Error) {
            pillState = PillState.Error
        } else {
            previousPillState = pillState
            pillState = PillState.MainScreen
        }
    }

    internal fun showDiscovery() {
        previousPillState = pillState
        pillState = PillState.Discovery
    }
}

public enum class PillState {
    MainScreen, NewPill, Discovery, Error
}