package com.programmersbox.common

import androidx.compose.runtime.Composable
import com.programmersbox.database.PillWeightDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import java.net.InetAddress

import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceListener


public actual fun getPlatformName(): String {
    return "Android"
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
}

internal actual fun doStuff() {
    GlobalScope.launch(Dispatchers.IO) {
        println("HERE!")
        val jmdns = JmDNS.create(InetAddress.getLocalHost())
        jmdns.addServiceListener("_http._tcp.local.", SampleListener())
        delay(30000)
    }
}

internal class SampleListener : ServiceListener {
    override fun serviceAdded(event: ServiceEvent) {
        println("Service added: " + event.info)
    }

    override fun serviceRemoved(event: ServiceEvent) {
        println("Service removed: " + event.info)
    }

    override fun serviceResolved(event: ServiceEvent) {
        println("Service resolved: " + event.info)
    }
}