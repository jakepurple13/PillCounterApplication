package com.programmersbox.common

import androidx.compose.runtime.Composable
import com.programmersbox.database.PillWeightDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

public actual fun getPlatformName(): String {
    return "Android"
}

@Composable
public fun UIShow() {
    App()
}

internal actual class Database actual constructor(scope: CoroutineScope) {
    private val db = PillWeightDatabase()
    actual suspend fun list(): Flow<List<PillWeights>> = db.getItems()
        .mapNotNull { l ->
            l.map { PillWeights(it.name, it.pillWeight, it.bottleWeight) }
        }
    actual suspend fun savePillWeightInfo(pillWeights: PillWeights) {
        db.saveInfo(pillWeights.name, pillWeights.pillWeight, pillWeights.bottleWeight)
    }
    actual suspend fun removePillWeightInfo(pillWeights: PillWeights) {
        db.removeInfo(pillWeights.name, pillWeights.pillWeight, pillWeights.bottleWeight)
    }
}