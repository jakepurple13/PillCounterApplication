package com.programmersbox.common

import kotlinx.serialization.Serializable

@Serializable
internal data class PillCount(
    val count: Double,
    val pillWeights: PillWeights
)

@Serializable
internal data class PillWeights(
    val name: String = "",
    val bottleWeight: Double = 0.0,
    val pillWeight: Double = 0.0
)