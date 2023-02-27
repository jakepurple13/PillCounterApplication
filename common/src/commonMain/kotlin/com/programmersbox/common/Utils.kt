package com.programmersbox.common

import androidx.compose.animation.*
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import moe.tlaster.precompose.navigation.NavOptions
import moe.tlaster.precompose.navigation.Navigator

@Composable
internal fun BannerBox(
    modifier: Modifier = Modifier,
    showBanner: Boolean = false,
    bannerEnter: EnterTransition = slideInVertically(
        animationSpec = tween(
            durationMillis = 150,
            easing = LinearOutSlowInEasing
        )
    ) { -it },
    bannerExit: ExitTransition = slideOutVertically(
        animationSpec = tween(
            durationMillis = 150,
            easing = LinearOutSlowInEasing
        )
    ) { -it },
    banner: @Composable BoxScope.() -> Unit
) {
    Box {
        AnimatedVisibility(
            visible = showBanner,
            enter = bannerEnter,
            exit = bannerExit,
            modifier = modifier
        ) { banner() }
    }
}

@Composable
internal fun BackButton() {
    val navigator = LocalNavigator.current
    IconButton(onClick = navigator::goBack) { Icon(Icons.Default.ArrowBack, null) }
}

internal fun Navigator.navigate(state: PillState, options: NavOptions? = null) = navigate(state.route, options)

internal fun Navigator.navigateToBLEDiscovery() = navigate(
    PillState.BluetoothDiscovery,
    NavOptions(
        launchSingleTop = true
    )
)

internal fun Navigator.navigateToDiscovery() = navigate(
    PillState.Discovery,
    NavOptions(
        launchSingleTop = true
    )
)

internal fun Navigator.navigateToNewPill() = navigate(
    PillState.NewPill,
    NavOptions(
        launchSingleTop = true
    )
)