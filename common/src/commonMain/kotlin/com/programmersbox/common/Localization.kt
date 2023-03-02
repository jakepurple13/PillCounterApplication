package com.programmersbox.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

public val LocalLocale: ProvidableCompositionLocal<Localization> =
    staticCompositionLocalOf { error("Nothing Here!") }

public class Localization(
    public val saved: String = "Saved",
    public val pills: @Composable (Double) -> String = { "${it.round(2)} pills" },
    public val updateCurrentConfig: String = "Update Current Config",
    public val saveCurrentConfig: String = "Save Current Config",
    public val home: String = "Home",
    public val addNewPill: String = "Add New Pill",
    public val areYouSureYouWantToRemoveThis: String = "Are you sre you want to remove this?",
    public val pillWeight: @Composable (Double) -> String = { "Pill Weight: ${it.round(2)}" },
    public val bottleWeight: @Composable (Double) -> String = { "Bottle Weight: ${it.round(2)}" },
    public val pillWeightCalibration: String = "Pill Weight (in grams)",
    public val bottleWeightCalibration: String = "Bottle Weight (in grams)",
    public val findPillCounter: String = "Find Pill Counter",
    public val retryConnection: String = "Retry Connection",
    public val somethingWentWrongWithTheConnection: String = "Something went wrong with the connection",
    public val connected: String = "Connected",
    public val id: @Composable (String) -> String = { "ID: $it" },
    public val pillName: String = "Pill Name",
    public val save: String = "Save",
    public val pressToStartCalibration: String = "Press to start calibration",
    public val discover: String = "Discover",
    public val enterIpAddress: String = "Enter ip address",
    public val manualIP: String = "Manual IP",
    public val discovery: String = "Discovery",
    public val needToConnectPillCounterToWifi: String = "Need to connect PillCounter to WiFi?",
    public val connect: String = "Connect",
    public val pleaseWait: String = "Please Wait...",
    public val ssid: String = "SSID",
    public val password: String = "Password",
    public val connectPillCounterToWiFi: String = "Connect PillCounter to Wifi",
    public val refreshNetworks: String = "Refresh Networks",
    public val releaseToRefresh: String = "Release To Refresh",
    public val refreshing: String = "Refreshing",
    public val pullToRefresh: String = "Pull to Refresh",
    public val close: String = "Close"
)