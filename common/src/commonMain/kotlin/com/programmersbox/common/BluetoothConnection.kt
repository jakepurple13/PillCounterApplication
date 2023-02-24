package com.programmersbox.common

import kotlinx.serialization.Serializable

@Serializable
internal data class ConnectRequest(
    val c: Int,
    val p: WiFiInfo
)

@Serializable
internal data class SingleCommand(val c: Int)

@Serializable
internal data class WiFiInfo(
    val e: String,
    val p: String
)

@Serializable
internal data class WifiList(
    var c: Int,
    var r: Int,
    var p: List<NetworkList> = emptyList()
)

@Serializable
internal data class NetworkList(
    var e: String? = null,
    var m: String? = null,
    var s: Int? = null,
    var p: Int? = null
)
