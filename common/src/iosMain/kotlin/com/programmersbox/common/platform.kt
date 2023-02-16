package com.programmersbox.common

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Application
import com.programmersbox.database.PillWeightDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
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
    actual suspend fun list(): Flow<List<PillWeights>> = db.getItems()
        .mapNotNull { l -> l.map { PillWeights(it.name, it.pillWeight, it.bottleWeight) } }

    actual suspend fun savePillWeightInfo(pillWeights: PillWeights) {
        db.saveInfo(pillWeights.name, pillWeights.pillWeight, pillWeights.bottleWeight)
    }

    actual suspend fun removePillWeightInfo(pillWeights: PillWeights) {
        db.removeInfo(pillWeights.name, pillWeights.pillWeight, pillWeights.bottleWeight)
    }

    actual suspend fun url(): Flow<String> = db.getUrl()
    actual suspend fun saveUrl(url: String) = db.saveUrl(url)
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