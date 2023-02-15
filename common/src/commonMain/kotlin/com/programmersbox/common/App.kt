package com.programmersbox.common

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MenuOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun App() {
    val scope = rememberCoroutineScope()
    val vm = remember { PillViewModel(scope) }
    Surface {
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
                        },
                        bottomBar = {
                            BottomAppBar(
                                actions = {},
                                floatingActionButton = {
                                    ExtendedFloatingActionButton(
                                        onClick = {
                                            when (vm.pillState) {
                                                PillState.MainScreen -> vm.showNewPill()
                                                PillState.NewPill -> vm.showMainScreen()
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                when (vm.pillState) {
                                                    PillState.MainScreen -> Icons.Default.Add
                                                    PillState.NewPill -> Icons.Default.Medication
                                                },
                                                null
                                            )
                                        },
                                        text = {
                                            Text(
                                                when (vm.pillState) {
                                                    PillState.MainScreen -> "Add New Pill"
                                                    PillState.NewPill -> "Return to Home Screen"
                                                }
                                            )
                                        }
                                    )
                                }
                            )
                        }
                    ) { padding ->
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            contentPadding = padding
                        ) {
                            items(vm.pillWeightList) {
                                ElevatedCard(
                                    onClick = {
                                        vm.onDrawerItemClick(PillWeights(it.name, it.bottleWeight, it.pillWeight))
                                    }
                                ) {
                                    ListItem(
                                        headlineText = { Text(it.name) },
                                        supportingText = {
                                            Column {
                                                Text("Bottle Weight: ${it.bottleWeight}")
                                                Text("Pill Weight: ${it.pillWeight}")
                                            }
                                        }
                                    )
                                }
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
                        navigationIcon = {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } }
                            ) { Icon(Icons.Default.MenuOpen, null) }
                        }
                    )
                },
                bottomBar = {
                    BottomAppBar {
                        NavigationBarItem(
                            selected = vm.pillState is PillState.MainScreen,
                            icon = { Icon(Icons.Default.Medication, null) },
                            label = { Text("Home") },
                            onClick = { vm.showMainScreen() }
                        )
                        NavigationBarItem(
                            selected = vm.pillState is PillState.NewPill,
                            icon = { Icon(Icons.Default.Add, null) },
                            label = { Text("Add New Pill") },
                            onClick = { vm.showNewPill() }
                        )
                    }
                }
            ) { padding ->
                Crossfade(vm.pillState) { target ->
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        when (target) {
                            PillState.MainScreen -> MainScreen(vm)
                            PillState.NewPill -> NewPill(vm)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BoxScope.NewPill(viewModel: PillViewModel) {
    Column(
        verticalArrangement = Arrangement.SpaceAround,
        modifier = Modifier
            .fillMaxSize()
            .align(Alignment.Center),
    ) {
        OutlinedTextField(
            value = viewModel.newPill.name,
            onValueChange = { viewModel.updatePill(name = it) },
            label = { Text("Pill Name") },
            modifier = Modifier.fillMaxWidth()
        )

        LinearProgressIndicator(
            progress = animateFloatAsState(
                targetValue = if(viewModel.isNewPillLoading) 0f else 1f,
                animationSpec = if(viewModel.isNewPillLoading) tween(5000) else tween(0)
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
                    Text(viewModel.newPill.pillWeight.toString() )
                    Text("Pill Weight (in grams)" )
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
                }
            }
        }

        OutlinedButton(
            onClick = {
                viewModel.saveNewConfig(viewModel.newPill)
                viewModel.sendNewConfig(viewModel.newPill)
                viewModel.showMainScreen()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !viewModel.isNewPillLoading
        ) { Text("Save") }
    }
}

@Composable
internal fun BoxScope.MainScreen(viewModel: PillViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Text(viewModel.pillCount.pillWeights.name)
        Text(
            "${viewModel.pillCount.count} pills",
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