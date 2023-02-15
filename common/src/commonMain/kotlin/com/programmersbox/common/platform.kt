package com.programmersbox.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

public expect fun getPlatformName(): String

internal expect class Database(scope: CoroutineScope) {
    suspend fun list(): Flow<List<PillWeights>>
    suspend fun savePillWeightInfo(pillWeights: PillWeights)
    suspend fun removePillWeightInfo(pillWeights: PillWeights)
}