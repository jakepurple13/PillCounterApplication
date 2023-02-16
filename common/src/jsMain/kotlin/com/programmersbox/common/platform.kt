package com.programmersbox.common

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
    actual suspend fun list(): Flow<List<PillWeights>> = flowOf(emptyList())
    actual suspend fun savePillWeightInfo(pillWeights: PillWeights) {}
    actual suspend fun removePillWeightInfo(pillWeights: PillWeights) {}
    actual suspend fun url(): Flow<String> = flowOf()
    actual suspend fun saveUrl(url: String) = Unit
}

internal actual fun DiscoveryViewModel.discover() {

}
