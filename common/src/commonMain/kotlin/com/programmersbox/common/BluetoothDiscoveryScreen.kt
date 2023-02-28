package com.programmersbox.common

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun <T, R> BluetoothDiscoveryScreen(
    state: BluetoothState,
    isConnecting: Boolean,
    connectOverBle: () -> Unit,
    device: T?,
    deviceList: List<T>,
    onDeviceClick: (T?) -> Unit,
    deviceIdentifier: (T?) -> String,
    deviceName: suspend (T?) -> String,
    isDeviceSelected: (found: T?, selected: T?) -> Boolean,
    networkItem: R?,
    onNetworkItemClick: (R?) -> Unit,
    wifiNetworks: List<R>,
    connectToWifi: (password: String) -> Unit,
    getNetworks: () -> Unit,
    ssid: (R?) -> String,
    signalStrength: (R?) -> Int,
    actions: @Composable RowScope.() -> Unit = {}
) {
    AnimatedContent(
        state,
        transitionSpec = {
            if (targetState > initialState) {
                slideInHorizontally { width -> width } + fadeIn() with
                        slideOutHorizontally { width -> -width } + fadeOut()
            } else {
                slideInHorizontally { width -> -width } + fadeIn() with
                        slideOutHorizontally { width -> width } + fadeOut()
            }.using(SizeTransform(clip = false))
        }
    ) { target ->
        when (target) {
            BluetoothState.Searching -> BluetoothSearching(
                isConnecting = isConnecting,
                connectOverBle = connectOverBle,
                device = device,
                deviceList = deviceList,
                onDeviceClick = onDeviceClick,
                name = deviceName,
                isSelected = isDeviceSelected,
                identifier = deviceIdentifier,
                actions = actions
            )

            BluetoothState.Wifi -> WifiConnect(
                networkItem = networkItem,
                onNetworkItemClick = onNetworkItemClick,
                wifiNetworks = wifiNetworks,
                getNetworks = getNetworks,
                connectToWifi = connectToWifi,
                ssid = ssid,
                signalStrength = signalStrength
            )

            BluetoothState.Checking -> {
                Surface {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Text("Please Wait...")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> BluetoothSearching(
    device: T?,
    deviceList: List<T>,
    onDeviceClick: (T?) -> Unit,
    identifier: (T?) -> String,
    name: suspend (T?) -> String,
    isSelected: (found: T?, selected: T?) -> Boolean,
    isConnecting: Boolean,
    connectOverBle: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Find PillCounter") },
                navigationIcon = { BackButton() },
                actions = actions
            )
        },
        bottomBar = {
            BottomAppBar {
                Button(
                    onClick = connectOverBle,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = device != null && !isConnecting
                ) { Text("Connect") }
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn {
                items(deviceList) {
                    Card(onClick = { onDeviceClick(it) }) {
                        var deviceName by remember { mutableStateOf("") }
                        LaunchedEffect(Unit) { deviceName = name(it) }
                        ListItem(
                            headlineText = { Text(identifier(it)) },
                            supportingText = { Text(deviceName) },
                            trailingContent = {
                                if (isSelected(it, device)) {
                                    Icon(Icons.Default.Check, null)
                                }
                            }
                        )
                    }
                }
            }
            if (isConnecting) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <R> WifiConnect(
    networkItem: R?,
    onNetworkItemClick: (R?) -> Unit,
    wifiNetworks: List<R>,
    connectToWifi: (password: String) -> Unit,
    getNetworks: () -> Unit,
    ssid: (R?) -> String,
    signalStrength: (R?) -> Int
) {
    var password by remember { mutableStateOf("") }
    var hidePassword by remember { mutableStateOf(true) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connect PillCounter to Wifi") },
                actions = {
                    AnimatedVisibility(
                        networkItem != null
                    ) {
                        IconButton(
                            onClick = { onNetworkItemClick(null) }
                        ) { Icon(Icons.Default.Close, null) }
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        icon = { Icon(Icons.Default.Send, null) },
                        text = { Text("Connect") },
                        onClick = { connectToWifi(password) }
                    )
                },
                actions = {
                    Button(
                        onClick = getNetworks,
                    ) {
                        Icon(Icons.Default.Refresh, null)
                        Text("Refresh Networks")
                    }
                }
            )
        }
    ) { padding ->
        Crossfade(networkItem) { target ->
            Box(Modifier.padding(padding)) {
                if (target != null) {
                    Column(modifier = Modifier.align(Alignment.Center)) {
                        OutlinedTextField(
                            value = ssid(target),
                            onValueChange = {},
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            label = { Text("SSID") },
                            leadingIcon = { Icon(Icons.Default.Wifi, null) },
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            singleLine = true,
                            visualTransformation = if (hidePassword) PasswordVisualTransformation() else VisualTransformation.None,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.WifiPassword, null) },
                            trailingIcon = {
                                IconToggleButton(
                                    hidePassword,
                                    onCheckedChange = { hidePassword = it }
                                ) {
                                    Icon(
                                        if (hidePassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        null
                                    )
                                }
                            }
                        )
                    }
                } else {
                    LazyColumn {
                        items(wifiNetworks) {
                            Card(onClick = { onNetworkItemClick(it) }) {
                                ListItem(
                                    headlineText = { Text(ssid(it)) },
                                    leadingContent = {
                                        Icon(
                                            when (signalStrength(it)) {
                                                in 0..25 -> Icons.Default.NetworkWifi1Bar
                                                in 25..50 -> Icons.Default.NetworkWifi2Bar
                                                in 50..75 -> Icons.Default.NetworkWifi3Bar
                                                in 50..75 -> Icons.Default.SignalWifi4Bar
                                                else -> Icons.Default.SignalWifiStatusbarNull
                                            },
                                            null
                                        )
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

public enum class BluetoothState {
    Searching, Wifi, Checking
}