package com.programmersbox.common

import androidx.compose.runtime.Composable
import com.programmersbox.database.PillWeightDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import java.net.InetAddress
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceListener

public actual fun getPlatformName(): String {
    return "Desktop"
}

@Composable
public fun UIShow() {
    App()
}

internal actual class Database actual constructor(scope: CoroutineScope) {
    private val db = PillWeightDatabase()
    actual suspend fun list(): Flow<List<PillWeights>> = db.getItems()
        .mapNotNull { l ->
            l.map { PillWeights(it.name, it.pillWeight, it.bottleWeight) }
        }

    actual suspend fun savePillWeightInfo(pillWeights: PillWeights) {
        db.saveInfo(pillWeights.name, pillWeights.pillWeight, pillWeights.bottleWeight)
    }

    actual suspend fun removePillWeightInfo(pillWeights: PillWeights) {
        db.removeInfo(pillWeights.name, pillWeights.pillWeight, pillWeights.bottleWeight)
    }

    actual suspend fun url(): Flow<String> = db.getUrl()
    actual suspend fun saveUrl(url: String) = db.saveUrl(url)

}

internal actual fun PillViewModel.doStuff() {

}

internal actual fun DiscoveryViewModel.discover() {
    isSearching = true
    scope.launch(Dispatchers.IO) {
        val jmdns = JmDNS.create(InetAddress.getLocalHost(), "HOST")
        jmdns.addServiceListener(
            "_http._tcp.local.",
            object : ServiceListener {
                override fun serviceAdded(event: ServiceEvent) {
                    println("Service added: " + event.info)
                }

                override fun serviceRemoved(event: ServiceEvent) {
                    println("Service removed: " + event.info)
                }

                override fun serviceResolved(event: ServiceEvent) {
                    println("Service resolved: " + event.info.inet4Addresses)
                    println("Service resolved: " + event.info.inet4Addresses.map { it.hostName })
                    println("Service resolved: " + event.info.inet4Addresses.map { it.hostAddress })
                    //val pillDevices = event.info.inet4Addresses.filter { it.canonicalHostName }
                    //println(pillDevices)
                    discoveredList.addAll(event.info.inet4Addresses.mapNotNull { it.hostAddress })
                }
            }
        )
        delay(30000)
        jmdns.unregisterAllServices()
        isSearching = false
    }
}