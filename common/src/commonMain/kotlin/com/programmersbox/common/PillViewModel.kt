package com.programmersbox.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

internal class PillViewModel(private val scope: CoroutineScope) {

    private val db = Database(scope)

    var pillCount by mutableStateOf(PillCount(0.0, PillWeights()))
        private set

    val pillWeightList = mutableStateListOf<PillWeights>()

    var pillState: PillState by mutableStateOf(PillState.MainScreen)

    var newPill by mutableStateOf(PillWeights())

    var isNewPillLoading by mutableStateOf(false)

    init {
        Network.socketConnection()
            .onEach { println(it) }
            .onEach { pillCount = it }
            .launchIn(scope)

        scope.launch {
            db.list()
                .onEach {
                    pillWeightList.clear()
                    pillWeightList.addAll(it)
                }
                .launchIn(this)
        }
    }

    fun onDrawerItemClick(pillWeights: PillWeights) {
        when (pillState) {
            PillState.MainScreen -> sendNewConfig(pillWeights)
            PillState.NewPill -> recalibrate(pillWeights)
        }
    }

    fun recalibrate(pillWeights: PillWeights) {
        newPill = pillWeights
    }

    fun updatePill(
        name: String = newPill.name,
        pillWeight: Double = newPill.pillWeight,
        bottleWeight: Double = newPill.bottleWeight
    ) {
        newPill = newPill.copy(name = name, bottleWeight = bottleWeight, pillWeight = pillWeight)
    }

    fun calibratePillWeight() {
        Network.pillWeightCalibration()
            .onStart { isNewPillLoading = true }
            .onEach { updatePill(pillWeight = it.pillWeight) }
            .onCompletion { isNewPillLoading = false }
            .catch { it.printStackTrace() }
            .launchIn(scope)
    }

    fun calibrateBottleWeight() {
        Network.pillWeightCalibration()
            .onStart { isNewPillLoading = true }
            .onEach { updatePill(bottleWeight = it.bottleWeight) }
            .onCompletion { isNewPillLoading = false }
            .catch { it.printStackTrace() }
            .launchIn(scope)
    }

    fun sendNewConfig(pillWeights: PillWeights) {
        scope.launch {
            Network.updateConfig(pillWeights)
                .onSuccess { println(it) }
                .onFailure { it.printStackTrace() }
        }
    }

    fun saveNewConfig(pillWeights: PillWeights) {
        scope.launch { db.savePillWeightInfo(pillWeights) }
    }

    fun showNewPill() {
        pillState = PillState.NewPill
    }

    fun showMainScreen() {
        pillState = PillState.MainScreen
    }
}

internal sealed class PillState {
    object MainScreen : PillState()
    object NewPill : PillState()
}