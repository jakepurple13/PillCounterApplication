package com.programmersbox.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import moe.tlaster.precompose.viewmodel.ViewModel
import moe.tlaster.precompose.viewmodel.viewModelScope

internal class NewPillViewModel(private val pillViewModel: PillViewModel) : ViewModel() {

    internal var newPill by mutableStateOf(PillWeights())

    internal var isNewPillLoading by mutableStateOf(false)

    private val network get() = pillViewModel.network
    private val db get() = pillViewModel.db

    fun recalibrate(pillWeights: PillWeights) {
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
            ?.launchIn(viewModelScope)
    }

    internal fun calibrateBottleWeight() {
        network?.pillWeightCalibration()
            ?.onStart { isNewPillLoading = true }
            ?.onEach { updatePill(bottleWeight = it.bottleWeight) }
            ?.onCompletion { isNewPillLoading = false }
            ?.catch { it.printStackTrace() }
            ?.launchIn(viewModelScope)
    }

    internal fun sendNewConfig(pillWeights: PillWeights) {
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
}