package com.programmersbox.common

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

internal object Network {

    private val json = Json {
        isLenient = true
        prettyPrint = true
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    val client = HttpClient {
        install(ContentNegotiation) { json(json) }
        install(WebSockets) { contentConverter = KotlinxWebsocketSerializationConverter(json) }
    }

    private suspend inline fun <reified T> getApi(
        url: String,
        noinline builder: HttpRequestBuilder.() -> Unit = {}
    ): T? {
        val response: HttpResponse = client.get(url, builder)
        return response.body<T>()
    }

    private suspend inline fun <reified T> postApi(
        url: String,
        noinline builder: HttpRequestBuilder.() -> Unit = {}
    ): T? {
        val response: HttpResponse = client.post(url, builder)
        return response.body<T>()
    }

    suspend fun updateConfig(pillWeights: PillWeights) = runCatching {
        postApi<PillWeights>("http://0.0.0.0:8080/weight") {
            contentType(ContentType.Application.Json)
            setBody(pillWeights)
        }
    }

    fun socketConnection(): Flow<PillCount> {
        val updateFlow = MutableSharedFlow<PillCount>()
        CoroutineScope(Job()).launch {
            client.ws(method = HttpMethod.Get, host = "0.0.0.0", port = 8080, path = "/ws") {
                incoming
                    .consumeAsFlow()
                    .filterIsInstance<Frame.Text>()
                    .map { it.readText() }
                    .mapNotNull { text ->
                        try {
                            json.decodeFromString<PillCount>(text)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }
                    }
                    .onEach { updateFlow.emit(it) }
                    .collect()
            }
        }
        return updateFlow
    }

    fun pillWeightCalibration(): Flow<PillWeights> = flow {
        withTimeout(5000) {
            while(true) {
                getApi<PillWeights>("http://0.0.0.0:8080/pillWeight")?.let { emit(it) }
                delay(10)
            }
        }
    }
}
