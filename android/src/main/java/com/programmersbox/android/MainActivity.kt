package com.programmersbox.android

import android.os.Build
import android.os.Bundle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.programmersbox.common.Localization
import com.programmersbox.common.UIShow
import moe.tlaster.precompose.lifecycle.PreComposeActivity
import moe.tlaster.precompose.lifecycle.setContent

class MainActivity : PreComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            CustomMaterialTheme {
                UIShow(
                    localization = Localization(
                        saved = stringResource(R.string.saved),
                        pills = { stringResource(R.string.pills, it) },
                        updateCurrentConfig = stringResource(R.string.updateCurrentConfig),
                        saveCurrentConfig = stringResource(R.string.saveCurrentConfig),
                        home = stringResource(R.string.home),
                        addNewPill = stringResource(R.string.addNewPill),
                        areYouSureYouWantToRemoveThis = stringResource(R.string.areYouSureYouWantToRemoveThis),
                        pillWeight = { stringResource(R.string.pillWeight, it) },
                        bottleWeight = { stringResource(R.string.bottleWeight, it) },
                        pillWeightCalibration = stringResource(R.string.pillWeightCalibration),
                        bottleWeightCalibration = stringResource(R.string.bottleWeightCalibration),
                        findPillCounter = stringResource(R.string.findPillCounter),
                        retryConnection = stringResource(R.string.retryConnection),
                        somethingWentWrongWithTheConnection = stringResource(R.string.somethingWentWrongWithTheConnection),
                        connected = stringResource(R.string.connected),
                        id = { stringResource(R.string.id_info) },
                        pillName = stringResource(R.string.pillName),
                        save = stringResource(R.string.save),
                        pressToStartCalibration = stringResource(R.string.pressToStartCalibration),
                        discover = stringResource(R.string.discover),
                        enterIpAddress = stringResource(R.string.enterIpAddress),
                        manualIP = stringResource(R.string.manualIP),
                        discovery = stringResource(R.string.discovery),
                        needToConnectPillCounterToWifi = stringResource(R.string.needToConnectPillCounterToWifi),
                        connect = stringResource(R.string.connect),
                        pleaseWait = stringResource(R.string.pleaseWait),
                        ssid = stringResource(R.string.ssid),
                        password = stringResource(R.string.password),
                        connectPillCounterToWiFi = stringResource(R.string.connectPillCounterToWiFi),
                        refreshNetworks = stringResource(R.string.refreshNetworks),
                        releaseToRefresh = stringResource(R.string.release_to_refresh),
                        refreshing = stringResource(R.string.refreshing),
                        pullToRefresh = stringResource(R.string.pull_to_refresh)
                    )
                )
            }
        }
    }
}

@Composable
fun CustomMaterialTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        val systemUiController = rememberSystemUiController()

        SideEffect {
            systemUiController.setNavigationBarColor(
                color = Color.Transparent,
                darkIcons = !darkTheme
            )
            systemUiController.setStatusBarColor(
                color = Color.Transparent,
                darkIcons = !darkTheme
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}