package com.programmersbox.common

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DiscoveryScreen(viewModel: PillViewModel) {
    val scope = rememberCoroutineScope()
    val vm = remember { DiscoveryViewModel(scope, viewModel) }
    var ip by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { vm.startDiscovery() }

    Scaffold(
        bottomBar = {
            Column {
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
            contentPadding = padding
        ) {
            items(vm.discoveredList) {
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
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Text("Searching")
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = { Text("Enter ip address") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    trailingIcon = {
                        IconButton(
                            onClick = { vm.connect(ip) }
                        ) { Icon(Icons.Default.Add, null) }
                    }
                )
            }
        }
    }
}

internal class DiscoveryViewModel(
    val scope: CoroutineScope,
    private val viewModel: PillViewModel
) {
    var isSearching by mutableStateOf(false)
    val discoveredList = mutableStateListOf<PillCounterIp>()

    fun startDiscovery() {
        if (!isSearching) {
            discover()
        }
    }

    fun connect(url: String) {
        viewModel.changeNetwork(url)
        viewModel.showMainScreen()
    }

}

internal data class PillCounterIp(val ip: String, val name: String)