package com.programmersbox.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NewPill(viewModel: NewPillViewModel) {
    val locale = LocalLocale.current
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
                ) { Text(locale.save) }
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
                label = { Text(locale.pillName) },
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
                        Text(locale.pillWeightCalibration)
                        Text(locale.pressToStartCalibration)
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
                        Text(locale.bottleWeightCalibration)
                        Text(locale.pressToStartCalibration)
                    }
                }
            }
        }
    }
}