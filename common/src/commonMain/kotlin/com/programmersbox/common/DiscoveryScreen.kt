package com.programmersbox.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DiscoveryScreen(viewModel: PillViewModel) {
    val scope = rememberCoroutineScope()
    val vm = remember { DiscoveryViewModel(scope, viewModel) }

    Scaffold(
        bottomBar = {
            BottomAppBar {
                OutlinedButton(
                    onClick = { vm.startDiscovery() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    Text("Discover")
                }
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