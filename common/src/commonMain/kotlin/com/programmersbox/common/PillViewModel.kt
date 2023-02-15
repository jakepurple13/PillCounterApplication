package com.programmersbox.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

public class PillViewModel(internal val scope: CoroutineScope) {

    private val db = Database(scope)

    private var network = Network()

    internal var pillCount by mutableStateOf(PillCount(0.0, PillWeights()))
        private set

    internal val pillWeightList = mutableStateListOf<PillWeights>()

    public var pillState: PillState by mutableStateOf(PillState.MainScreen)

    internal var newPill by mutableStateOf(PillWeights())

    internal var isNewPillLoading by mutableStateOf(false)

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
            db.url()
                .filter { it.isNotEmpty() }
                .onEach { connectToNetwork(it) }
                .launchIn(this)
        }
    }

    internal fun changeNetwork(url: String) {
        scope.launch { db.saveUrl(url) }
    }

    private fun connectToNetwork(url: String) {
        network.close()

        network = Network(Url("http://$url:8080"))

        network.socketConnection()
            .onEach { println(it) }
            .onEach { pillCount = it }
            .launchIn(scope)
    }

    internal fun onDrawerItemClick(pillWeights: PillWeights) {
        when (pillState) {
            PillState.MainScreen -> sendNewConfig(pillWeights)
            PillState.NewPill -> recalibrate(pillWeights)
            PillState.Discovery -> {}
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
        network.pillWeightCalibration()
            .onStart { isNewPillLoading = true }
            .onEach { updatePill(pillWeight = it.pillWeight) }
            .onCompletion { isNewPillLoading = false }
            .catch { it.printStackTrace() }
            .launchIn(scope)
    }

    internal fun calibrateBottleWeight() {
        network.pillWeightCalibration()
            .onStart { isNewPillLoading = true }
            .onEach { updatePill(bottleWeight = it.bottleWeight) }
            .onCompletion { isNewPillLoading = false }
            .catch { it.printStackTrace() }
            .launchIn(scope)
    }

    internal fun sendNewConfig(pillWeights: PillWeights) {
        scope.launch {
            network.updateConfig(pillWeights)
                .onSuccess { println(it) }
                .onFailure { it.printStackTrace() }
        }
    }

    internal fun saveNewConfig(pillWeights: PillWeights) {
        scope.launch { db.savePillWeightInfo(pillWeights) }
    }

    internal fun removeConfig(pillWeights: PillWeights) {
        scope.launch { db.removePillWeightInfo(pillWeights) }
    }

    internal fun showNewPill() {
        pillState = PillState.NewPill
    }

    public fun showMainScreen() {
        pillState = PillState.MainScreen
    }

    internal fun showDiscovery() {
        pillState = PillState.Discovery
    }
}

public sealed class PillState {
    public object MainScreen : PillState()
    public object NewPill : PillState()
    public object Discovery : PillState()
}