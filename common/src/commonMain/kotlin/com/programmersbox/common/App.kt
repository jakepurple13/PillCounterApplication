package com.programmersbox.common

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import moe.tlaster.precompose.navigation.NavHost
import moe.tlaster.precompose.navigation.Navigator
import moe.tlaster.precompose.navigation.rememberNavigator
import moe.tlaster.precompose.navigation.transition.NavTransition
import moe.tlaster.precompose.ui.viewModel

internal val LocalNavigator = staticCompositionLocalOf<Navigator> { error("No NavController Found!") }

@Composable
internal fun App(
    navigator: Navigator = rememberNavigator(),
    scope: CoroutineScope = rememberCoroutineScope(),
    vm: PillViewModel = remember { PillViewModel(navigator, scope) },
    backHandler: @Composable (PillViewModel) -> Unit = {}
) {
    backHandler(vm)

    CompositionLocalProvider(
        LocalNavigator provides navigator,
    ) {
        Surface {
            NavHost(
                navigator = navigator,
                initialRoute = PillState.MainScreen.route,
            ) {
                scene(
                    PillState.MainScreen.route,
                    navTransition = NavTransition(
                        createTransition = slideInHorizontally { width -> -width },
                        resumeTransition = slideInHorizontally { width -> -width },
                        pauseTransition = slideOutHorizontally { width -> width },
                        destroyTransition = slideOutHorizontally { width -> width }
                    )
                ) {
                    DrawerInfo(
                        vm = vm,
                        drawerClick = vm::onDrawerItemMainScreenClick,
                        homeSelected = true
                    ) { MainScreen(vm) }
                }
                scene(
                    PillState.NewPill.route,
                    navTransition = NavTransition(
                        createTransition = slideInHorizontally { width -> -width },
                        resumeTransition = slideInHorizontally { width -> -width },
                        pauseTransition = slideOutHorizontally { width -> width },
                        destroyTransition = slideOutHorizontally { width -> width }
                    )
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
                        createTransition = slideInVertically { height -> height },
                        resumeTransition = slideInVertically { height -> height },
                        pauseTransition = slideOutVertically { height -> -height },
                        destroyTransition = slideOutHorizontally { height -> -height }
                    )
                ) { BluetoothDiscovery(vm) }
                scene(
                    PillState.Discovery.route,
                    navTransition = NavTransition(
                        createTransition = slideInVertically { height -> height },
                        resumeTransition = slideInVertically { height -> height },
                        pauseTransition = slideOutVertically { height -> -height },
                        destroyTransition = slideOutHorizontally { height -> -height }
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
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Saved") },
                            actions = {
                                IconButton(
                                    onClick = { scope.launch { drawerState.close() } }
                                ) { Icon(Icons.Default.Close, null) }
                            }
                        )
                    }
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
                                            headlineText = { Text("Are you sre you want to remove this?") },
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
                                                    Text("Bottle Weight: ${it.pillWeights.bottleWeight}")
                                                    Text("Pill Weight: ${it.pillWeights.pillWeight}")
                                                }
                                            },
                                            overlineText = { Text("ID: ${it.pillWeights.uuid}") },
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
                                    "Connected",
                                    color = MaterialTheme.colorScheme.surface
                                )
                            }
                        }
                    }

                    BannerBox(
                        showBanner = vm.connectedState == ConnectionState.Error
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
                                Text("Something went wrong with the connection")
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
                                    ) { Text("Find Pill Counter") }
                                    Button(
                                        onClick = vm::reconnect,
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error
                                        ),
                                        enabled = !vm.isConnectionLoading
                                    ) { Text("Retry Connection") }
                                }
                                if (vm.isConnectionLoading) CircularProgressIndicator()
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
                        label = { Text("Home") },
                        onClick = { vm.showMainScreen() }
                    )
                    NavigationBarItem(
                        selected = newPillSelected,
                        icon = { Icon(Icons.Default.Add, null) },
                        label = { Text("Add New Pill") },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NewPill(viewModel: NewPillViewModel) {
    val navigator = LocalNavigator.current
    Scaffold(
        bottomBar = {
            BottomAppBar {
                OutlinedButton(
                    onClick = {
                        val pill = if (viewModel.newPill.uuid.isEmpty()) {
                            viewModel.newPill.copy(uuid = randomUUID())
                        } else {
                            viewModel.newPill
                        }
                        viewModel.saveNewConfig(pill)
                        viewModel.sendNewConfig(pill)
                        navigator.goBack()
                        viewModel.newPill = PillWeights()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    enabled = !viewModel.isNewPillLoading
                ) { Text("Save") }
            }
        }
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.SpaceAround,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OutlinedTextField(
                value = viewModel.newPill.name,
                onValueChange = { viewModel.updatePill(name = it) },
                label = { Text("Pill Name") },
                modifier = Modifier.fillMaxWidth()
            )

            LinearProgressIndicator(
                progress = animateFloatAsState(
                    targetValue = if (viewModel.isNewPillLoading) 0f else 1f,
                    animationSpec = if (viewModel.isNewPillLoading) tween(5000) else tween(0)
                ).value,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedCard(
                    onClick = { viewModel.calibratePillWeight() },
                    enabled = !viewModel.isNewPillLoading,
                    modifier = Modifier
                        .height(100.dp)
                        .weight(1f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(viewModel.newPill.pillWeight.toString())
                        Text("Pill Weight (in grams)")
                        Text("Press to start calibration")
                    }
                }

                OutlinedCard(
                    onClick = { viewModel.calibrateBottleWeight() },
                    enabled = !viewModel.isNewPillLoading,
                    modifier = Modifier
                        .height(100.dp)
                        .weight(1f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(viewModel.newPill.bottleWeight.toString())
                        Text("Bottle Weight (in grams)")
                        Text("Press to start calibration")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainScreen(viewModel: PillViewModel) {
    Scaffold(
        bottomBar = {
            BottomAppBar {
                if (viewModel.pillAlreadySaved) {
                    OutlinedButton(
                        onClick = { viewModel.updateConfig(viewModel.pillCount) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) { Text("Update Current Config") }
                } else {
                    OutlinedButton(
                        onClick = { viewModel.saveNewConfig(viewModel.pillCount.pillWeights) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) { Text("Save Current Config") }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(viewModel.pillCount.pillWeights.name)
            Text(
                "${viewModel.pillCount.formattedCount()} pills",
                style = MaterialTheme.typography.titleLarge
            )
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Pill Weight: ${viewModel.pillCount.pillWeights.pillWeight}",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "Bottle Weight: ${viewModel.pillCount.pillWeights.bottleWeight}",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}