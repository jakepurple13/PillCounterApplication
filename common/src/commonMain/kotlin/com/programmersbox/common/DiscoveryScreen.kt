package com.programmersbox.common

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import moe.tlaster.precompose.ui.viewModel
import moe.tlaster.precompose.viewmodel.ViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DiscoveryScreen(viewModel: PillViewModel) {
    val navigator = LocalNavigator.current
    val vm = viewModel { DiscoveryViewModel(viewModel) }
    var ip by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { vm.startDiscovery() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discovery") },
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
                    ) { Text("Need to Connect PillCounter to WiFi?") }
                }
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = { Text("Enter ip address") },
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
                        ) { Text("Discover") }
                    },
                    floatingActionButton = {
                        ExtendedFloatingActionButton(
                            text = { Text("Manual IP") },
                            icon = { Icon(Icons.Default.Add, null) },
                            onClick = { if (ip.isNotEmpty()) vm.connect(ip) }
                        )
                    }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(4.dp)
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

            item {
                AnimatedVisibility(
                    vm.isSearching,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Text("Searching")
                    }
                }
            }
        }
    }
}

internal class DiscoveryViewModel(
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
        viewModel.showMainScreen()
    }

}

internal data class PillCounterIp(val ip: String, val name: String)