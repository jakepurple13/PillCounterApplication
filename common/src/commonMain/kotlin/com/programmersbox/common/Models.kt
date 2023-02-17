package com.programmersbox.common

import kotlinx.serialization.Serializable
import kotlin.math.round

@Serializable
internal data class PillCount(
    val count: Double,
    val pillWeights: PillWeights
) {
    fun formattedCount() = count.round(2)
}

@Serializable
internal data class PillWeights(
    val name: String = "",
    val bottleWeight: Double = 0.0,
    val pillWeight: Double = 0.0,
    val uuid: String = ""
)

internal fun Double.round(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return round(this * multiplier) / multiplier
}