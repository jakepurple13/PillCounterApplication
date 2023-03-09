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
import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.HiddenFromObjC

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

@OptIn(ExperimentalObjCRefinement::class)
@HiddenFromObjC
@Composable
public fun Color.animate(): State<Color> = animateColorAsState(this)

@OptIn(ExperimentalObjCRefinement::class)
@HiddenFromObjC
public val Emerald: Color = Color(0xFF2ecc71)

@OptIn(ExperimentalObjCRefinement::class)
@HiddenFromObjC
public val Sunflower: Color = Color(0xFFf1c40f)

@OptIn(ExperimentalObjCRefinement::class)
@HiddenFromObjC
public val Alizarin: Color = Color(0xFFe74c3c)


@OptIn(ExperimentalObjCRefinement::class)
@HiddenFromObjC
public fun Navigator.navigate(state: PillState, options: NavOptions? = null): Unit = navigate(state.route, options)

@OptIn(ExperimentalObjCRefinement::class)
@HiddenFromObjC
public fun Navigator.navigateToBLEDiscovery(): Unit = navigate(
    PillState.BluetoothDiscovery,
    NavOptions(
        launchSingleTop = true
    )
)

@OptIn(ExperimentalObjCRefinement::class)
@HiddenFromObjC
public fun Navigator.navigateToDiscovery(): Unit = navigate(
    PillState.Discovery,
    NavOptions(
        launchSingleTop = true
    )
)

@OptIn(ExperimentalObjCRefinement::class)
@HiddenFromObjC
public fun Navigator.navigateToNewPill(): Unit = navigate(
    PillState.NewPill,
    NavOptions(
        launchSingleTop = true
    )
)

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class, ExperimentalObjCRefinement::class)
@HiddenFromObjC
@Composable
public fun SwipeToRemove(
    onRemoveClick: () -> Unit,
    onClick: () -> Unit,
    headlineText: @Composable () -> Unit = {},
    supportingText: (@Composable () -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    overlineText: (@Composable () -> Unit)? = null,
    removingSupporting: @Composable () -> Unit = {},
    elevatedCardColors: CardColors = CardDefaults.elevatedCardColors(),
    elevatedListItemColors: ListItemColors = ListItemDefaults.colors()
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
                        IconButton(onClick = onRemoveClick) {
                            Icon(Icons.Default.Check, null)
                        }
                    }
                )
            }
        } else {
            ElevatedCard(
                onClick = onClick,
                colors = elevatedCardColors
            ) {
                ListItem(
                    colors = elevatedListItemColors,
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