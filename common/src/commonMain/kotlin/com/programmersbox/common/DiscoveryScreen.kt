package com.programmersbox.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import moe.tlaster.precompose.navigation.BackHandler
import moe.tlaster.precompose.navigation.Navigator
import moe.tlaster.precompose.ui.viewModel
import moe.tlaster.precompose.viewmodel.ViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DiscoveryScreen(viewModel: PillViewModel) {
    val locale = LocalLocale.current
    val navigator = LocalNavigator.current
    val vm = viewModel(DiscoveryViewModel::class) { DiscoveryViewModel(navigator, viewModel) }
    var ip by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    LaunchedEffect(Unit) { vm.startDiscovery() }

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
                        MediumTopAppBar(
                            title = { Text(locale.pastDevices) },
                            scrollBehavior = scrollBehavior
                        )
                    },
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                ) { padding ->
                    LazyColumn(
                        contentPadding = padding
                    ) {
                        items(viewModel.urlHistory) {
                            SwipeToRemove(
                                item = it,
                                onRemoveClick = viewModel::removeUrl,
                                onClick = vm::connect,
                                headlineText = { Text(it) },
                                removingSupporting = { Text(it) },
                                leadingContent = {
                                    if (it == viewModel.url) {
                                        Icon(Icons.Default.Check, null)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(locale.discovery) },
                    navigationIcon = { BackButton() },
                    actions = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } }
                        ) { Icon(Icons.Default.History, null) }
                    }
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier.background(MaterialTheme.colorScheme.background)
                ) {
                    if (hasBLEDiscovery) {
                        OutlinedButton(
                            onClick = { navigator.navigateToBLEDiscovery() },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(locale.needToConnectPillCounterToWifi) }
                    }
                    OutlinedTextField(
                        value = ip,
                        onValueChange = { ip = it },
                        label = { Text(locale.enterIpAddress) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                            .padding(bottom = 4.dp),
                        trailingIcon = {
                            IconButton(
                                onClick = { vm.connect(ip) }
                            ) { Icon(Icons.Default.Add, null) }
                        }
                    )
                    LinearProgressIndicator(
                        progress = animateFloatAsState(
                            targetValue = if (vm.isSearching) 0f else 1f,
                            animationSpec = if (vm.isSearching) tween(30000) else tween(0)
                        ).value,
                        modifier = Modifier.fillMaxWidth()
                    )
                    BottomAppBar(
                        actions = {
                            OutlinedButton(
                                onClick = { vm.startDiscovery() },
                                enabled = !vm.isSearching,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                            ) { Text(locale.discover) }
                        },
                        floatingActionButton = {
                            ExtendedFloatingActionButton(
                                text = { Text(locale.manualIP) },
                                icon = { Icon(Icons.Default.Add, null) },
                                onClick = { if (ip.isNotEmpty()) vm.connect(ip) }
                            )
                        }
                    )
                }
            }
        ) { padding ->
            PullToRefresh(
                state = rememberPullToRefreshState(vm.isSearching),
                onRefresh = vm::discover,
                indicatorPadding = padding,
                indicator = { state, refreshTrigger, refreshingOffset ->
                    PullToRefreshIndicator(
                        state,
                        refreshTrigger,
                        refreshingOffset,
                        releaseToRefresh = locale::releaseToRefresh,
                        refreshing = locale::refreshing,
                        pullToRefresh = locale::pullToRefresh
                    )
                }
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = padding,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(vm.filteredDiscoveredList) {
                        OutlinedCard(
                            onClick = { vm.connect(it.ip) }
                        ) {
                            ListItem(
                                headlineText = { Text(it.name) },
                                supportingText = { Text(it.ip) }
                            )
                        }
                    }
                }
            }
        }
    }
}

internal class DiscoveryViewModel(
    private val navigator: Navigator,
    private val viewModel: PillViewModel
) : ViewModel() {
    var isSearching by mutableStateOf(false)
    val discoveredList = mutableStateListOf<PillCounterIp>()
    val filteredDiscoveredList by derivedStateOf { discoveredList.distinct() }

    fun startDiscovery() {
        if (!isSearching) {
            discoveredList.clear()
            discover()
        }
    }

    fun connect(url: String) {
        viewModel.changeNetwork(url)
        navigator.goBack()
    }

}

internal data class PillCounterIp(val ip: String, val name: String)