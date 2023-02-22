package com.programmersbox.common

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Application
import com.programmersbox.database.PillWeightDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import platform.Foundation.NSUUID
import platform.UIKit.UIViewController

public actual fun getPlatformName(): String {
    return "iOS"
}

@Composable
private fun UIShow() {
    App()
}

internal var discoverAction: ((List<IpInfo>) -> Unit) -> Unit = {}

public fun MainViewController(
    actionOnDiscover: ((List<IpInfo>) -> Unit) -> Unit = {}
): UIViewController {
    discoverAction = actionOnDiscover
    return Application("PillCounter") {
        MaterialTheme(
            colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Spacer(Modifier.height(30.dp))
                    UIShow()
                }
            }
        }
    }
}

internal actual class Database actual constructor(scope: CoroutineScope) {
    private val db = PillWeightDatabase()
    actual suspend fun list(): Flow<List<PillCount>> = db.getItems()
        .mapNotNull { l ->
            l.map {
                PillCount(
                    count = it.currentCount,
                    PillWeights(
                        name = it.name,
                        pillWeight = it.pillWeight,
                        bottleWeight = it.bottleWeight,
                        uuid = it.uuid
                    )
                )
            }
        }

    actual suspend fun savePillWeightInfo(pillWeights: PillWeights) {
        db.saveInfo(
            name = pillWeights.name,
            pillWeight = pillWeights.pillWeight,
            bottleWeight = pillWeights.bottleWeight,
            uuid = pillWeights.uuid
        )
    }

    actual suspend fun removePillWeightInfo(pillWeights: PillWeights) {
        db.removeInfo(pillWeights.uuid)
    }

    actual suspend fun url(): Flow<String> = db.getUrl()
    actual suspend fun saveUrl(url: String) = db.saveUrl(url)
    actual suspend fun updateInfo(pillCount: PillCount) {
        db.updateInfo(pillCount.pillWeights.uuid) {
            currentCount = pillCount.count
            pillWeight = pillCount.pillWeights.pillWeight
            bottleWeight = pillCount.pillWeights.bottleWeight
            name = pillCount.pillWeights.name
        }
    }

    actual suspend fun updateCurrentCountInfo(pillCount: PillCount) {
        db.updateInfo(pillCount.pillWeights.uuid) {
            currentCount = pillCount.count
        }
    }

    actual suspend fun currentPill(): Flow<PillCount> = db.getLatest()
        .map {
            PillCount(
                count = it.currentCount,
                PillWeights(
                    name = it.name,
                    pillWeight = it.pillWeight,
                    bottleWeight = it.bottleWeight,
                    uuid = it.uuid
                )
            )
        }

    actual suspend fun updateCurrentPill(pillCount: PillCount) {
        db.updateLatest(
            currentCount = pillCount.count,
            name = pillCount.pillWeights.name,
            bottleWeight = pillCount.pillWeights.bottleWeight,
            pillWeight = pillCount.pillWeights.pillWeight,
            uuid = pillCount.pillWeights.uuid,
        )
    }
}

internal actual fun DiscoveryViewModel.discover() {
    isSearching = true
    scope.launch {
        async {
            discoverAction { list ->
                discoveredList.addAll(list.map { PillCounterIp(it.host, it.name) }.distinct())
            }
        }.await()
        delay(30000)
        isSearching = false
    }
}

public class IpInfo(public val name: String, public val host: String)

internal actual fun randomUUID(): String = NSUUID.UUID().UUIDString()

@Composable
internal actual fun BerryLanButton() {
    val uriHandler = LocalUriHandler.current
    OutlinedButton(
        onClick = { uriHandler.openUri("https://itunes.apple.com/us/app/berrylan/id1436156018") },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("BerryLan to Connect PillCounter to WiFi") }
}