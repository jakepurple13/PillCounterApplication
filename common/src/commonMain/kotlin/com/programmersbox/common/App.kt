package com.programmersbox.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import moe.tlaster.precompose.navigation.BackHandler
import moe.tlaster.precompose.navigation.NavHost
import moe.tlaster.precompose.navigation.Navigator
import moe.tlaster.precompose.navigation.rememberNavigator
import moe.tlaster.precompose.navigation.transition.NavTransition
import moe.tlaster.precompose.ui.viewModel
import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.HiddenFromObjC

@OptIn(ExperimentalObjCRefinement::class)
@HiddenFromObjC
public val LocalNavigator: ProvidableCompositionLocal<Navigator> =
    staticCompositionLocalOf { error("No NavController Found!") }

@OptIn(ExperimentalObjCRefinement::class)
@HiddenFromObjC
public val LocalLocale: ProvidableCompositionLocal<Localization> = staticCompositionLocalOf { error("Nothing Here!") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun App(
    localization: Localization = Localization(),
    navigator: Navigator = rememberNavigator(),
    scope: CoroutineScope = rememberCoroutineScope(),
    vm: PillViewModel = remember { PillViewModel(navigator, scope) },
) {
    CompositionLocalProvider(
        LocalNavigator provides navigator,
        LocalLocale provides localization
    ) {
        Surface {
            NavHost(
                navigator = navigator,
                initialRoute = PillState.MainScreen.route,
            ) {
                scene(
                    PillState.MainScreen.route,
                ) {
                    DrawerInfo(
                        showNewPill = vm::showNewPill,
                        showMainScreen = vm::showMainScreen,
                        showDiscovery = vm::showDiscovery,
                        reconnect = vm::reconnect,
                        removeConfig = vm::removeConfig,
                        connectionError = vm.connectionError,
                        showErrorBannerChange = { vm.showErrorBanner = it },
                        showErrorBanner = vm.showErrorBanner,
                        pillWeightList = vm.pillWeightList,
                        connectedState = vm.connectedState,
                        drawerClick = vm::onDrawerItemMainScreenClick,
                        homeSelected = true
                    ) {
                        HomeScreen(
                            pillCount = vm.pillCount,
                            pillAlreadySaved = vm.pillAlreadySaved,
                            updateConfig = vm::updateConfig,
                            saveNewConfig = vm::saveNewConfig
                        )
                    }
                }
                scene(
                    PillState.NewPill.route,
                ) {
                    val newPillVm = viewModel(NewPillViewModel::class) { NewPillViewModel(vm) }
                    DrawerInfo(
                        showNewPill = vm::showNewPill,
                        showMainScreen = vm::showMainScreen,
                        showDiscovery = vm::showDiscovery,
                        reconnect = vm::reconnect,
                        removeConfig = vm::removeConfig,
                        connectionError = vm.connectionError,
                        showErrorBannerChange = { vm.showErrorBanner = it },
                        showErrorBanner = vm.showErrorBanner,
                        pillWeightList = vm.pillWeightList,
                        connectedState = vm.connectedState,
                        drawerClick = newPillVm::recalibrate,
                        newPillSelected = true
                    ) { NewPill(newPillVm) }
                }
                scene(
                    PillState.BluetoothDiscovery.route,
                    navTransition = NavTransition(
                        createTransition = slideInVertically { height -> -height },
                        resumeTransition = slideInVertically { height -> -height },
                        pauseTransition = slideOutVertically { height -> height },
                        destroyTransition = slideOutVertically { height -> height }
                    )
                ) { BluetoothDiscovery(vm) }
                scene(
                    PillState.Discovery.route,
                    navTransition = NavTransition(
                        createTransition = slideInVertically { height -> -height },
                        resumeTransition = slideInVertically { height -> -height },
                        pauseTransition = slideOutVertically { height -> height },
                        destroyTransition = slideOutVertically { height -> height }
                    )
                ) {
                    DiscoveryDrawer(
                        locale = localization,
                        urlHistory = vm.urlHistory,
                        removeUrl = vm::removeUrl,
                        connect = {
                            vm.changeNetwork(it)
                            navigator.goBack()
                        },
                        currentUrl = vm.url,
                    ) {
                        DiscoveryScreen(
                            drawerState = it,
                            changeNetwork = vm::changeNetwork
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DrawerInfo(
    showNewPill: () -> Unit,
    showMainScreen: () -> Unit,
    showDiscovery: () -> Unit,
    reconnect: () -> Unit,
    removeConfig: (PillWeights) -> Unit,
    connectionError: Boolean,
    showErrorBannerChange: (Boolean) -> Unit,
    showErrorBanner: Boolean,
    pillWeightList: List<PillCount>,
    connectedState: ConnectionState,
    homeSelected: Boolean = false,
    newPillSelected: Boolean = false,
    drawerClick: (PillWeights) -> Unit,
    content: @Composable () -> Unit
) {
    val locale = LocalLocale.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    if (drawerState.isOpen) {
        BackHandler { scope.launch { drawerState.close() } }
    }

    DrawerType(
        drawerState = drawerState,
        drawerContent = {
            val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(locale.saved) },
                        actions = {
                            IconButton(
                                onClick = { scope.launch { drawerState.close() } }
                            ) { Icon(Icons.Default.Close, null) }
                        },
                        scrollBehavior = scrollBehavior
                    )
                },
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
            ) { padding ->
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    contentPadding = padding
                ) {
                    items(pillWeightList) {
                        SwipeToRemove(
                            onRemoveClick = { removeConfig(it.pillWeights) },
                            onClick = { drawerClick(it.pillWeights) },
                            removingSupporting = { Text(it.pillWeights.name) },
                            headlineText = { Text(it.pillWeights.name) },
                            supportingText = {
                                Column {
                                    Text(locale.bottleWeight(it.pillWeights.bottleWeight))
                                    Text(locale.pillWeight(it.pillWeights.pillWeight))
                                }
                            },
                            leadingContent = { Text(it.formattedCount().toString()) },
                            overlineText = { Text(locale.id(it.pillWeights.uuid)) },
                        )
                    }
                }
            }
        },
        content = {
            Scaffold(
                topBar = {
                    Column {
                        TopAppBar(
                            title = { Text("Pill Counter") },
                            navigationIcon = {
                                IconButton(
                                    onClick = { scope.launch { drawerState.open() } }
                                ) { Icon(Icons.Default.MenuOpen, null) }
                            },
                            actions = {
                                AnimatedVisibility(connectionError) {
                                    IconButton(
                                        onClick = { showErrorBannerChange(true) },
                                        colors = IconButtonDefaults.iconButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        ),
                                        enabled = !showErrorBanner
                                    ) { Icon(Icons.Default.Warning, null) }
                                }
                                IconButton(
                                    onClick = showDiscovery,
                                    colors = IconButtonDefaults.iconButtonColors(
                                        contentColor = (if (connectionError) MaterialTheme.colorScheme.error else Emerald)
                                            .animate().value
                                    )
                                ) { Icon(Icons.Default.Wifi, null) }
                            },
                            colors = TopAppBarDefaults.smallTopAppBarColors(
                                containerColor = if (connectionError) {
                                    MaterialTheme.colorScheme.errorContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }.animate().value,
                            )
                        )

                        BannerBox(
                            showBanner = connectedState == ConnectionState.Connected
                        ) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .animateContentSize()
                                    .fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium.copy(
                                    topStart = CornerSize(0.dp),
                                    topEnd = CornerSize(0.dp)
                                ),
                                tonalElevation = 4.dp,
                                shadowElevation = 10.dp,
                                color = Emerald
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                ) {
                                    Text(
                                        locale.connected,
                                        color = MaterialTheme.colorScheme.surface
                                    )
                                }
                            }
                        }

                        BannerBox(
                            showBanner = showErrorBanner
                        ) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .animateContentSize()
                                    .fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium.copy(
                                    topStart = CornerSize(0.dp),
                                    topEnd = CornerSize(0.dp)
                                ),
                                tonalElevation = 4.dp,
                                shadowElevation = 10.dp,
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                ) {
                                    Text(locale.somethingWentWrongWithTheConnection)
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    ) {
                                        Button(
                                            onClick = showDiscovery,
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error
                                            )
                                        ) { Text(locale.findPillCounter) }
                                        Button(
                                            onClick = reconnect,
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error
                                            )
                                        ) { Text(locale.retryConnection) }
                                    }
                                    TextButton(
                                        onClick = { showErrorBannerChange(false) },
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        ),
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    ) { Text(locale.close) }
                                }
                            }
                        }
                    }
                },
                bottomBar = {
                    BottomAppBar {
                        NavigationBarItem(
                            selected = homeSelected,
                            icon = { Icon(Icons.Default.Medication, null) },
                            label = { Text(locale.home) },
                            onClick = showMainScreen
                        )
                        NavigationBarItem(
                            selected = newPillSelected,
                            icon = { Icon(Icons.Default.Add, null) },
                            label = { Text(locale.addNewPill) },
                            onClick = showNewPill
                        )
                    }
                }
            ) { padding ->
                Box(Modifier.padding(padding)) {
                    content()
                }
            }
        }
    )
}