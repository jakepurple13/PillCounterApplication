package com.programmersbox.common

import androidx.compose.runtime.Composable
import com.programmersbox.database.PillWeightDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import java.net.InetAddress
import java.util.*
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceListener


public actual fun getPlatformName(): String {
    return "Android"
}

@Composable
public fun UIShow(
    backHandler: @Composable (PillViewModel) -> Unit = {}
) {
    App(backHandler)
}

internal actual class Database actual constructor(scope: CoroutineScope) {
    private val db = PillWeightDatabase()
    actual suspend fun list(): Flow<List<PillCount>> = db.getItems()
        .mapNotNull { l ->
            l.map {
                PillCount(
                    count = it.currentCount,
                    PillWeights(
                        name = it.name,
                        pillWeight = it.pillWeight,
                        bottleWeight = it.bottleWeight,
                        uuid = it.uuid
                    )
                )
            }
        }

    actual suspend fun savePillWeightInfo(pillWeights: PillWeights) {
        db.saveInfo(
            name = pillWeights.name,
            pillWeight = pillWeights.pillWeight,
            bottleWeight = pillWeights.bottleWeight,
            uuid = pillWeights.uuid
        )
    }

    actual suspend fun removePillWeightInfo(pillWeights: PillWeights) {
        db.removeInfo(pillWeights.uuid)
    }

    actual suspend fun url(): Flow<String> = db.getUrl()
    actual suspend fun saveUrl(url: String) = db.saveUrl(url)
    actual suspend fun updateInfo(pillCount: PillCount) {
        db.updateInfo(pillCount.pillWeights.uuid) {
            currentCount = pillCount.count
            pillWeight = pillCount.pillWeights.pillWeight
            bottleWeight = pillCount.pillWeights.bottleWeight
            name = pillCount.pillWeights.name
        }
    }

    actual suspend fun updateCurrentCountInfo(pillCount: PillCount) {
        db.updateInfo(pillCount.pillWeights.uuid) {
            currentCount = pillCount.count
        }
    }

    actual suspend fun currentPill(): Flow<PillCount> = db.getLatest()
        .map {
            PillCount(
                count = it.currentCount,
                PillWeights(
                    name = it.name,
                    pillWeight = it.pillWeight,
                    bottleWeight = it.bottleWeight,
                    uuid = it.uuid
                )
            )
        }

    actual suspend fun updateCurrentPill(pillCount: PillCount) {
        db.updateLatest(
            currentCount = pillCount.count,
            name = pillCount.pillWeights.name,
            bottleWeight = pillCount.pillWeights.bottleWeight,
            pillWeight = pillCount.pillWeights.pillWeight,
            uuid = pillCount.pillWeights.uuid,
        )
    }
}

internal actual fun DiscoveryViewModel.discover() {
    isSearching = true
    scope.launch(Dispatchers.IO) {
        val jmdns = JmDNS.create(InetAddress.getByName("10.0.0.2"), "HOST")
        jmdns.addServiceListener(
            "_http._tcp.local.",
            object : ServiceListener {
                override fun serviceAdded(event: ServiceEvent) {
                    //println("Service added: " + event.info)
                }

                override fun serviceRemoved(event: ServiceEvent) {
                    //println("Service removed: " + event.info)
                }

                override fun serviceResolved(event: ServiceEvent) {
                    //println("Service resolved: " + event.info.inet4Addresses)
                    //println("Service resolved: " + event.info.inet4Addresses.map { it.hostName })
                    //println("Service resolved: " + event.info.inet4Addresses.map { it.hostAddress })
                    //val pillDevices = event.info.inet4Addresses.filter { it.canonicalHostName }
                    //println(pillDevices)
                    discoveredList.addAll(
                        event.info.inet4Addresses.mapNotNull {
                            it.hostAddress?.let { it1 -> PillCounterIp(it1, it.hostName) }
                        }
                    )
                }
            }
        )
        delay(30000)
        jmdns.unregisterAllServices()
        isSearching = false
    }
}

internal actual fun randomUUID(): String = UUID.randomUUID().toString()