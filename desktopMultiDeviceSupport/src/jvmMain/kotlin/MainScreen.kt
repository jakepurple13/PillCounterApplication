import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiFind
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.programmersbox.common.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    desktopViewModel: DesktopViewModel,
    locale: Localization,
    vm: MultiPillViewModel,
    onShowDiscovery: (Boolean) -> Unit
) {
    PermanentNavigationDrawer(
        drawerContent = {
            PermanentDrawerSheet {
                val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(locale.saved) },
                            scrollBehavior = scrollBehavior
                        )
                    },
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                ) { padding ->
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        contentPadding = padding
                    ) {
                        stickyHeader { TopAppBar(title = { Text("Devices") }) }
                        items(vm.network.values.toList()) {
                            val containerColor = when (it.connectionState) {
                                ConnectionState.Error -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.surface
                            }.animate().value

                            val isLowOnPills = it.pillCount.count <= desktopViewModel.alertThreshold &&
                                    it.pillCount.count > 0 && it.pillCount.pillWeights.uuid.isNotEmpty()

                            SwipeToRemove(
                                onRemoveClick = { vm.removeUrl(it.ip) },
                                onClick = {
                                    when (it.connectionState) {
                                        ConnectionState.Error -> vm.reconnect(it.ip)
                                        ConnectionState.Connected -> vm.setCurrentPillCount(it)
                                        else -> {}
                                    }
                                },
                                headlineText = { Text(it.ip) },
                                removingSupporting = { Text(it.ip) },
                                supportingText = {
                                    Column {
                                        Text(it.pillCount.count.toString())
                                        Text(it.pillCount.pillWeights.name)
                                    }
                                },
                                overlineText = {
                                    when {
                                        it.connectionState == ConnectionState.Error -> Text("Press to Try to Reconnect")
                                        isLowOnPills -> Text("Low on Pills")
                                    }
                                },
                                leadingContent = {
                                    Column {
                                        Icon(
                                            when (it.connectionState) {
                                                ConnectionState.Connected -> Icons.Default.Wifi
                                                ConnectionState.Error -> Icons.Default.WifiOff
                                                ConnectionState.Idle -> Icons.Default.WifiFind
                                            }, null
                                        )
                                        AnimatedVisibility(isLowOnPills) {
                                            Icon(
                                                Icons.Default.Medication,
                                                null,
                                                tint = Alizarin
                                            )
                                        }
                                    }
                                },
                                elevatedCardColors = CardDefaults.elevatedCardColors(
                                    containerColor = containerColor
                                ),
                                elevatedListItemColors = ListItemDefaults.colors(
                                    containerColor = containerColor
                                )
                            )
                        }
                        stickyHeader { TopAppBar(title = { Text("Pills") }) }
                        items(vm.pillWeightList) {
                            var showDialog by remember { mutableStateOf(false) }
                            if (showDialog) {
                                SendingDialog(
                                    pillCount = it,
                                    networks = vm.network.values.toList(),
                                    onSend = { ip ->
                                        vm.onDrawerItemMainScreenClick(it.pillWeights, ip)
                                    },
                                    onDismissRequest = { showDialog = false }
                                )
                            }
                            SwipeToRemove(
                                onRemoveClick = { vm.removeConfig(it.pillWeights) },
                                onClick = { showDialog = true },
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
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Pill Counter") },
                    actions = {
                        IconButton(
                            onClick = { onShowDiscovery(true) }
                        ) { Icon(Icons.Default.Wifi, null) }
                    }
                )
            }
        ) { padding ->
            HomeScreen(
                vm.pillCount,
                vm.pillAlreadySaved,
                vm::updateConfig,
                vm::saveNewConfig,
                modifier = Modifier.padding(padding)
            )
        }
    }
}