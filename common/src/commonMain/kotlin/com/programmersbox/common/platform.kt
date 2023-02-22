package com.programmersbox.common

import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

public expect fun getPlatformName(): String

internal expect class Database(scope: CoroutineScope) {
    suspend fun list(): Flow<List<PillCount>>
    suspend fun url(): Flow<String>
    suspend fun currentPill(): Flow<PillCount>
    suspend fun updateCurrentPill(pillCount: PillCount)
    suspend fun saveUrl(url: String)
    suspend fun savePillWeightInfo(pillWeights: PillWeights)
    suspend fun removePillWeightInfo(pillWeights: PillWeights)
    suspend fun updateInfo(pillCount: PillCount)
    suspend fun updateCurrentCountInfo(pillCount: PillCount)
}

internal expect fun DiscoveryViewModel.discover()

internal expect fun randomUUID(): String

@Composable
internal expect fun BerryLanButton()