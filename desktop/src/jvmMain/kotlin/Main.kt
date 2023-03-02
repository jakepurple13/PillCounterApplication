import androidx.compose.animation.core.animateFloatAsState
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
import com.programmersbox.common.Localization
import com.programmersbox.common.PillViewModel
import com.programmersbox.common.UIShow
import com.programmersbox.common.round
import moe.tlaster.precompose.navigation.Navigator
import java.text.MessageFormat
import java.util.*
import java.util.prefs.Preferences

fun main() = application {
    val scope = rememberCoroutineScope()
    val navigator = remember { Navigator() }
    val pillViewModel = remember { PillViewModel(navigator, scope) }
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
            desktopViewModel.bundle.getString(
                "pillCount",
                animateFloatAsState(pillCount.count.toFloat()).value.toDouble().round(2)
            ),
            onClick = {}
        )
        Item(
            desktopViewModel.locale.pillWeight(pillCount.pillWeights.pillWeight),
            onClick = {}
        )
        Item(
            desktopViewModel.locale.bottleWeight(pillCount.pillWeights.bottleWeight),
            onClick = {}
        )
        Separator()
        Item(
            desktopViewModel.bundle.getString(if (isOpen) "closeWindow" else "openWindow"),
            onClick = { isOpen = !isOpen }
        )
        Item(
            desktopViewModel.bundle.getString("preferences"),
            onClick = { openPreferences = !openPreferences }
        )
        Item(
            desktopViewModel.bundle.getString("quit"),
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
                    title = desktopViewModel.bundle.getString("lowOnPills", pillCount.pillWeights.name),
                    message = desktopViewModel.bundle.getString("youHaveLeft", pillCount.count)
                )
            )
        }
    }

    if (isOpen) {
        WindowWithBar(
            onCloseRequest = { isOpen = false },
            windowTitle = "PillCounter",
        ) { UIShow(desktopViewModel.locale, navigator, pillViewModel) }
    }

    if (openPreferences) {
        WindowWithBar(
            onCloseRequest = { openPreferences = false },
            windowTitle = desktopViewModel.bundle.getString("settings"),
        ) { PreferencesWindow(desktopViewModel) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreferencesWindow(desktopViewModel: DesktopViewModel) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(desktopViewModel.bundle.getString("settings")) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ListItem(
                headlineText = { Text(desktopViewModel.bundle.getString("alertThreshold")) },
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

internal fun ResourceBundle.getString(key: String, vararg params: Any) =
    MessageFormat.format(getString(key), *params)

internal class DesktopViewModel {

    private val preferences = Preferences.userNodeForPackage(DesktopViewModel::class.java)

    internal val bundle by lazy { ResourceBundle.getBundle("strings") }

    val locale by lazy {
        Localization(
            saved = bundle.getString("saved"),
            pills = { bundle.getString("pills", it.round(2)) },
            updateCurrentConfig = bundle.getString("updateCurrentConfig"),
            saveCurrentConfig = bundle.getString("saveCurrentConfig"),
            home = bundle.getString("home"),
            addNewPill = bundle.getString("addNewPill"),
            areYouSureYouWantToRemoveThis = bundle.getString("areYouSureYouWantToRemoveThis"),
            pillWeight = { bundle.getString("pillWeight", it.round(2)) },
            bottleWeight = { bundle.getString("bottleWeight", it.round(2)) },
            pillWeightCalibration = bundle.getString("pillWeightCalibration"),
            bottleWeightCalibration = bundle.getString("bottleWeightCalibration"),
            findPillCounter = bundle.getString("findPillCounter"),
            retryConnection = bundle.getString("retryConnection"),
            somethingWentWrongWithTheConnection = bundle.getString("somethingWentWrongWithTheConnection"),
            connected = bundle.getString("connected"),
            id = { bundle.getString("id_info", it) },
            pillName = bundle.getString("pillName"),
            save = bundle.getString("save"),
            pressToStartCalibration = bundle.getString("pressToStartCalibration"),
            discover = bundle.getString("discover"),
            enterIpAddress = bundle.getString("enterIpAddress"),
            manualIP = bundle.getString("manualIP"),
            discovery = bundle.getString("discovery"),
            needToConnectPillCounterToWifi = bundle.getString("needToConnectPillCounterToWifi"),
            connect = bundle.getString("connect"),
            pleaseWait = bundle.getString("pleaseWait"),
            ssid = bundle.getString("ssid"),
            password = bundle.getString("password"),
            connectPillCounterToWiFi = bundle.getString("connectPillCounterToWiFi"),
            refreshNetworks = bundle.getString("refreshNetworks"),
            releaseToRefresh = bundle.getString("release_to_refresh"),
            refreshing = bundle.getString("refreshing"),
            pullToRefresh = bundle.getString("pull_to_refresh"),
            close = bundle.getString("close")
        )
    }

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