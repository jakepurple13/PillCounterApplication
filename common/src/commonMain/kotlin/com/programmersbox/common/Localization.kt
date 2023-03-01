package com.programmersbox.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

public val LocalLocale: ProvidableCompositionLocal<Localization> =
    staticCompositionLocalOf { error("Nothing Here!") }

public class Localization(
    internal val saved: String = "Saved",
    internal val pills: @Composable (Double) -> String = { "${it.round(2)} pills" },
    internal val updateCurrentConfig: String = "Update Current Config",
    internal val saveCurrentConfig: String = "Save Current Config",
    internal val home: String = "Home",
    internal val addNewPill: String = "Add New Pill",
    internal val areYouSureYouWantToRemoveThis: String = "Are you sre you want to remove this?",
    internal val pillWeight: @Composable (Double) -> String = { "Pill Weight: ${it.round(2)}" },
    internal val bottleWeight: @Composable (Double) -> String = { "Bottle Weight: ${it.round(2)}" },
    internal val pillWeightCalibration: String = "Pill Weight (in grams)",
    internal val bottleWeightCalibration: String = "Bottle Weight (in grams)",
    internal val findPillCounter: String = "Find Pill Counter",
    internal val retryConnection: String = "Retry Connection",
    internal val somethingWentWrongWithTheConnection: String = "Something went wrong with the connection",
    internal val connected: String = "Connected",
    internal val id: @Composable (String) -> String = { "ID: $it" },
    internal val pillName: String = "Pill Name",
    internal val save: String = "Save",
    internal val pressToStartCalibration: String = "Press to start calibration",
    internal val discover: String = "Discover",
    internal val enterIpAddress: String = "Enter ip address",
    internal val manualIP: String = "Manual IP",
    internal val discovery: String = "Discovery",
    internal val needToConnectPillCounterToWifi: String = "Need to connect PillCounter to WiFi?",
    internal val connect: String = "Connect",
    internal val pleaseWait: String = "Please Wait...",
    internal val ssid: String = "SSID",
    internal val password: String = "Password",
    internal val connectPillCounterToWiFi: String = "Connect PillCounter to Wifi",
    internal val refreshNetworks: String = "Refresh Networks",
    internal val releaseToRefresh: String = "Release To Refresh",
    internal val refreshing: String = "Refreshing",
    internal val pullToRefresh: String = "Pull to Refresh",
    internal val close: String = "Close"
)