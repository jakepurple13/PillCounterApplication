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
import com.programmersbox.common.*
import moe.tlaster.precompose.navigation.Navigator
import java.text.MessageFormat
import java.util.*
import java.util.prefs.Preferences

@OptIn(ExperimentalMaterial3Api::class)
fun main() = application {
    //TODO: Maaaaybe try making a multiple device supported version?
    // also for all the drawers, make them expect so that desktop can have permanent drawers
    // maybe make a new module for this

    //TODO: ALSO! On the watchdog, maybe have it print the watchdog logs in a different color

    val scope = rememberCoroutineScope()
    val navigator = remember { Navigator() }
    val pillViewModel = remember { MultiPillViewModel(navigator, scope) }
    val desktopViewModel = remember { DesktopViewModel() }
    var isOpen by remember { mutableStateOf(true) }
    var openPreferences by remember { mutableStateOf(false) }
    var openDiscovery by remember { mutableStateOf(false) }
    val pillCount = pillViewModel.pillCount

    CompositionLocalProvider(
        LocalNavigator provides navigator,
        LocalLocale provides desktopViewModel.locale
    ) {

        val trayState = rememberTrayState()
        Tray(
            state = trayState,
            icon = rememberVectorPainter(Icons.Default.Medication),
        ) {
            Menu(
                "Connected Devices"
            ) {
                pillViewModel.network.forEach {
                    Item(
                        it.key,
                        onClick = {
                            if (it.value.connectionState == ConnectionState.Error) pillViewModel.reconnect(it.key)
                            else pillViewModel.setCurrentPillCount(it.value)
                        }
                    )
                }
            }
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
            ) {
                MainScreen(
                    desktopViewModel.locale,
                    pillViewModel
                ) { openDiscovery = it }
            }
        }

        if (openDiscovery) {
            WindowWithBar(
                onCloseRequest = { openDiscovery = false },
                windowTitle = "Discovery",
            ) {
                DiscoveryScreen { pillViewModel.changeNetwork(it) }
            }
        }

        if (openPreferences) {
            WindowWithBar(
                onCloseRequest = { openPreferences = false },
                windowTitle = desktopViewModel.bundle.getString("settings"),
            ) { PreferencesWindow(desktopViewModel) }
        }
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
