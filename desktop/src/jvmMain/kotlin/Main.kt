import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberTrayState
import com.programmersbox.common.PillViewModel
import com.programmersbox.common.UIShow
import java.util.prefs.Preferences

fun main() = application {
    val scope = rememberCoroutineScope()
    val pillViewModel = remember { PillViewModel(scope) }
    val desktopViewModel = remember { DesktopViewModel() }
    var isOpen by remember { mutableStateOf(true) }
    var openPreferences by remember { mutableStateOf(false) }

    val pillCount = pillViewModel.pillCount

    val trayState = rememberTrayState()
    Tray(
        state = trayState,
        icon = rememberVectorPainter(Icons.Default.Medication),
    ) {
        Item(
            pillCount.pillWeights.name,
            onClick = {}
        )
        Item(
            "Pill Count: ${pillCount.formattedCount()}",
            onClick = {}
        )
        Item(
            "Pill Weight: ${pillCount.pillWeights.pillWeight}",
            onClick = {}
        )
        Item(
            "Bottle Weight: ${pillCount.pillWeights.bottleWeight}",
            onClick = {}
        )
        Separator()
        Item(
            if (isOpen) "Close PillCounter Window" else "Open PillCounter Window",
            onClick = { isOpen = !isOpen }
        )
        Item(
            "Preferences",
            onClick = { openPreferences = !openPreferences }
        )
        Item(
            "Quit",
            onClick = ::exitApplication
        )
    }

    LaunchedEffect(desktopViewModel.alertThreshold, pillCount) {
        if (
            pillCount.count <= desktopViewModel.alertThreshold &&
            pillCount.count > 0 && pillCount.pillWeights.uuid.isNotEmpty()
        ) {
            trayState.sendNotification(
                Notification(
                    title = "Low on ${pillCount.pillWeights.name}",
                    message = "You have ~${pillCount.count} left"
                )
            )
        }
    }

    if (isOpen) {
        WindowWithBar(
            onCloseRequest = { isOpen = false },
            windowTitle = "PillCounter",
        ) { UIShow(scope, pillViewModel) }
    }

    if (openPreferences) {
        WindowWithBar(
            onCloseRequest = { openPreferences = false },
            windowTitle = "PillCounter Settings",
        ) { PreferencesWindow(desktopViewModel) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreferencesWindow(desktopViewModel: DesktopViewModel) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ListItem(
                headlineText = { Text("Alert Threshold") },
                supportingText = {
                    Column {
                        Text(desktopViewModel.alertThreshold.toString())
                        Slider(
                            value = desktopViewModel.alertThreshold.toFloat(),
                            onValueChange = { desktopViewModel.setThreshold(it.toInt()) },
                            valueRange = 0f..50f,
                            steps = 50
                        )
                    }
                }
            )
            Divider()
        }
    }
}

internal class DesktopViewModel {

    private val preferences = Preferences.userNodeForPackage(DesktopViewModel::class.java)

    var alertThreshold by mutableStateOf(preferences.getInt(PILL_COUNTER_ALERT_THRESHOLD, 10))
        private set

    fun setThreshold(i: Int) {
        alertThreshold = i
        preferences.putInt(PILL_COUNTER_ALERT_THRESHOLD, i)
    }

    companion object {
        private const val PILL_COUNTER_ALERT_THRESHOLD = "pillCounterAlertThreshold"
    }
}