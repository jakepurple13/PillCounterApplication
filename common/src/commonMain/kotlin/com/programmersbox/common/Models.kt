package com.programmersbox.common

import kotlinx.serialization.Serializable
import kotlin.math.round

@Serializable
public data class PillCount(
    val count: Double,
    val pillWeights: PillWeights
) {
    public fun formattedCount(): Double = count.round(2)
}

@Serializable
public data class PillWeights(
    val name: String = "",
    val bottleWeight: Double = 0.0,
    val pillWeight: Double = 0.0,
    val uuid: String = ""
)

public fun Double.round(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return round(this * multiplier) / multiplier
}

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
public data class NetworkList(
    var e: String? = null,
    var m: String? = null,
    var s: Int? = null,
    var p: Int? = null
)
