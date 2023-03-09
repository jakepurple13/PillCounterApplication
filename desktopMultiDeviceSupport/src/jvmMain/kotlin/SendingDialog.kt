import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiFind
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberDialogState
import androidx.compose.ui.window.rememberWindowState
import com.programmersbox.common.ConnectionState
import com.programmersbox.common.Emerald
import com.programmersbox.common.PillCount
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.hostOs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SendingDialog(
    pillCount: PillCount,
    networks: List<NetworkInfo>,
    onSend: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    var networkToSend by remember { mutableStateOf("") }
    Dialog(
        onCloseRequest = {
            onDismissRequest()
            networkToSend = ""
        },
        undecorated = true,
        transparent = true,
        state = rememberDialogState(size = DpSize(600.dp, 500.dp))
    ) {
        val state = rememberWindowState()
        val hasFocus = LocalWindowInfo.current.isWindowFocused
        Surface(
            shape = when (hostOs) {
                OS.Linux -> RoundedCornerShape(8.dp)
                OS.Windows -> RectangleShape
                OS.MacOS -> RoundedCornerShape(8.dp)
                else -> RoundedCornerShape(8.dp)
            },
            modifier = Modifier.animateContentSize(),
            border = ButtonDefaults.outlinedButtonBorder,
        ) {
            Scaffold(
                topBar = {
                    Column {
                        WindowDraggableArea(
                            modifier = Modifier.combinedClickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = {},
                                onDoubleClick = {
                                    state.placement =
                                        if (state.placement != WindowPlacement.Maximized) {
                                            WindowPlacement.Maximized
                                        } else {
                                            WindowPlacement.Floating
                                        }
                                }
                            )
                        ) {
                            androidx.compose.material.TopAppBar(
                                backgroundColor = animateColorAsState(
                                    if (hasFocus) MaterialTheme.colorScheme.surface
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ).value,
                                elevation = 0.dp,
                            ) {
                                NativeTopBar(state, onDismissRequest, "Send to Device", showMinimize = false)
                            }
                        }
                        Divider(color = MaterialTheme.colorScheme.onSurface)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
            ) { dialogPadding ->
                Scaffold(
                    topBar = { CenterAlignedTopAppBar(title = { Text("Send ${pillCount.pillWeights.name}") }) },
                    bottomBar = {
                        BottomAppBar {
                            Row(
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TextButton(
                                    onClick = {
                                        onSend(networkToSend)
                                        onDismissRequest()
                                        networkToSend = ""
                                    },
                                    enabled = networkToSend.isNotEmpty()
                                ) { Text("Send") }

                                TextButton(
                                    onClick = {
                                        onDismissRequest()
                                        networkToSend = ""
                                    }
                                ) { Text("Cancel") }
                            }
                        }
                    },
                    modifier = Modifier.padding(dialogPadding)
                ) { padding ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 2.dp),
                        contentPadding = padding,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(networks) {
                            val isSending = it.ip == networkToSend
                            val selectedContainer = animateColorAsState(
                                if (isSending) Emerald else MaterialTheme.colorScheme.surface
                            ).value
                            val selectedContent = animateColorAsState(
                                if (isSending) Color.DarkGray else MaterialTheme.colorScheme.onSurface
                            ).value
                            ElevatedCard(
                                onClick = { networkToSend = if (isSending) "" else it.ip },
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = selectedContainer,
                                    contentColor = selectedContent
                                )
                            ) {
                                ListItem(
                                    colors = ListItemDefaults.colors(
                                        containerColor = selectedContainer,
                                        headlineColor = selectedContent,
                                        leadingIconColor = selectedContent
                                    ),
                                    headlineText = { Text(it.ip) },
                                    leadingContent = {
                                        Icon(
                                            when (it.connectionState) {
                                                ConnectionState.Connected -> Icons.Default.Wifi
                                                ConnectionState.Error -> Icons.Default.WifiOff
                                                ConnectionState.Idle -> Icons.Default.WifiFind
                                            }, null
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