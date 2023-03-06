package com.programmersbox.common

import androidx.compose.animation.*
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import moe.tlaster.precompose.navigation.NavOptions
import moe.tlaster.precompose.navigation.Navigator

@Composable
internal fun BannerBox(
    modifier: Modifier = Modifier,
    showBanner: Boolean = false,
    bannerEnter: EnterTransition = expandVertically(
        animationSpec = tween(
            durationMillis = 150,
            easing = LinearOutSlowInEasing
        )
    ),
    bannerExit: ExitTransition = shrinkVertically(
        animationSpec = tween(
            durationMillis = 150,
            easing = LinearOutSlowInEasing
        )
    ),
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

@Composable
internal fun Color.animate() = animateColorAsState(this)

internal val Emerald = Color(0xFF2ecc71)

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

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun <T> SwipeToRemove(
    item: T,
    onRemoveClick: (T) -> Unit,
    onClick: (T) -> Unit,
    headlineText: @Composable () -> Unit = {},
    supportingText: (@Composable () -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    overlineText: (@Composable () -> Unit)? = null,
    removingSupporting: @Composable () -> Unit = {},
) {
    val locale = LocalLocale.current
    var remove by remember { mutableStateOf(false) }
    AnimatedContent(
        remove,
        transitionSpec = {
            if (targetState > initialState) {
                slideInHorizontally { width -> -width } + fadeIn() with
                        slideOutHorizontally { width -> width } + fadeOut()
            } else {
                slideInHorizontally { width -> width } + fadeIn() with
                        slideOutHorizontally { width -> -width } + fadeOut()
            }.using(SizeTransform(clip = false))
        }
    ) { target ->
        if (target) {
            OutlinedCard(
                border = BorderStroke(
                    CardDefaults.outlinedCardBorder().width,
                    Color.Red
                )
            ) {
                ListItem(
                    leadingContent = {
                        IconButton(onClick = { remove = false }) {
                            Icon(Icons.Default.Close, null)
                        }
                    },
                    headlineText = { Text(locale.areYouSureYouWantToRemoveThis) },
                    supportingText = removingSupporting,
                    trailingContent = {
                        IconButton(onClick = { onRemoveClick(item) }) {
                            Icon(Icons.Default.Check, null)
                        }
                    }
                )
            }
        } else {
            ElevatedCard(
                onClick = { onClick(item) }
            ) {
                ListItem(
                    headlineText = headlineText,
                    supportingText = supportingText,
                    overlineText = overlineText,
                    leadingContent = leadingContent,
                    trailingContent = {
                        IconButton(onClick = { remove = true }) {
                            Icon(Icons.Default.Delete, null)
                        }
                    }
                )
            }
        }
    }
}