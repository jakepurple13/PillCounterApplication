package com.programmersbox.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(viewModel: PillViewModel) {
    val locale = LocalLocale.current
    Scaffold(
        bottomBar = {
            BottomAppBar {
                if (viewModel.pillAlreadySaved) {
                    OutlinedButton(
                        onClick = { viewModel.updateConfig(viewModel.pillCount) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) { Text(locale.updateCurrentConfig) }
                } else {
                    OutlinedButton(
                        onClick = { viewModel.saveNewConfig(viewModel.pillCount.pillWeights) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) { Text(locale.saveCurrentConfig) }
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
                locale.pills(viewModel.pillCount.formattedCount()),
                style = MaterialTheme.typography.titleLarge
            )
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    locale.pillWeight(viewModel.pillCount.pillWeights.pillWeight),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    locale.bottleWeight(viewModel.pillCount.pillWeights.bottleWeight),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}