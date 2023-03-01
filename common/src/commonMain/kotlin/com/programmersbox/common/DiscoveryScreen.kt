package com.programmersbox.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

    LaunchedEffect(Unit) { vm.startDiscovery() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(locale.discovery) },
                navigationIcon = { BackButton() }
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