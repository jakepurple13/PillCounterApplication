package com.programmersbox.common

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
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

internal val LocalNavigator = staticCompositionLocalOf<Navigator> { error("No NavController Found!") }
internal val LocalLocale: ProvidableCompositionLocal<Localization> = staticCompositionLocalOf { error("Nothing Here!") }

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
                        vm = vm,
                        drawerClick = vm::onDrawerItemMainScreenClick,
                        homeSelected = true
                    ) { HomeScreen(vm) }
                }
                scene(
                    PillState.NewPill.route,
                ) {
                    val newPillVm = viewModel(NewPillViewModel::class) { NewPillViewModel(vm) }
                    DrawerInfo(
                        vm = vm,
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
                ) { DiscoveryScreen(vm) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
internal fun DrawerInfo(
    vm: PillViewModel,
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
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
                        items(vm.pillWeightList) {
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
                                            supportingText = { Text(it.pillWeights.name) },
                                            trailingContent = {
                                                IconButton(onClick = { vm.removeConfig(it.pillWeights) }) {
                                                    Icon(Icons.Default.Check, null)
                                                }
                                            }
                                        )
                                    }
                                } else {
                                    ElevatedCard(
                                        onClick = { drawerClick(it.pillWeights) }
                                    ) {
                                        ListItem(
                                            headlineText = { Text(it.pillWeights.name) },
                                            supportingText = {
                                                Column {
                                                    Text(locale.bottleWeight(it.pillWeights.bottleWeight))
                                                    Text(locale.pillWeight(it.pillWeights.pillWeight))
                                                }
                                            },
                                            overlineText = { Text(locale.id(it.pillWeights.uuid)) },
                                            leadingContent = { Text(it.formattedCount().toString()) },
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
                    }
                }
            }
        }
    ) {
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
                            AnimatedVisibility(vm.connectionError) {
                                IconButton(
                                    onClick = { vm.showErrorBanner = true },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    enabled = !vm.showErrorBanner
                                ) { Icon(Icons.Default.Warning, null) }
                            }
                            IconButton(
                                onClick = { vm.showDiscovery() },
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = (if (vm.connectionError) MaterialTheme.colorScheme.error else Emerald)
                                        .animate().value
                                )
                            ) { Icon(Icons.Default.Wifi, null) }
                        },
                        colors = TopAppBarDefaults.smallTopAppBarColors(
                            containerColor = if (vm.connectionError) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }.animate().value,
                        )
                    )

                    BannerBox(
                        showBanner = vm.connectedState == ConnectionState.Connected
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
                        showBanner = vm.showErrorBanner
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
                                        onClick = vm::showDiscovery,
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error
                                        )
                                    ) { Text(locale.findPillCounter) }
                                    Button(
                                        onClick = vm::reconnect,
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error
                                        )
                                    ) { Text(locale.retryConnection) }
                                }
                                TextButton(
                                    onClick = { vm.showErrorBanner = false },
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
                        onClick = { vm.showMainScreen() }
                    )
                    NavigationBarItem(
                        selected = newPillSelected,
                        icon = { Icon(Icons.Default.Add, null) },
                        label = { Text(locale.addNewPill) },
                        onClick = { vm.showNewPill() }
                    )
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding)) {
                content()
            }
        }
    }
}