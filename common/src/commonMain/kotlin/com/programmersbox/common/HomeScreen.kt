package com.programmersbox.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.HiddenFromObjC

@OptIn(ExperimentalMaterial3Api::class, ExperimentalObjCRefinement::class)
@HiddenFromObjC
@Composable
public fun HomeScreen(
    pillCount: PillCount,
    pillAlreadySaved: Boolean,
    updateConfig: (PillCount) -> Unit,
    saveNewConfig: (PillWeights) -> Unit,
    topBar: @Composable () -> Unit = {}
) {
    val locale = LocalLocale.current
    Scaffold(
        topBar = topBar,
        bottomBar = {
            BottomAppBar {
                if (pillAlreadySaved) {
                    OutlinedButton(
                        onClick = { updateConfig(pillCount) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) { Text(locale.updateCurrentConfig) }
                } else {
                    OutlinedButton(
                        onClick = { saveNewConfig(pillCount.pillWeights) },
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
            Text(pillCount.pillWeights.name)
            Text(
                locale.pills(animateFloatAsState(pillCount.count.toFloat()).value.toDouble().round(2)),
                style = MaterialTheme.typography.titleLarge
            )
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    locale.pillWeight(pillCount.pillWeights.pillWeight),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    locale.bottleWeight(pillCount.pillWeights.bottleWeight),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}