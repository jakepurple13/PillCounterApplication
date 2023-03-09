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
import io.ktor.utils.io.errors.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

public class Network(
    private val url: Url = Url("http://${BuildKonfig.serverLocalIpAddress}:8080")
) {
    private val json = Json {
        isLenient = true
        prettyPrint = true
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) { json(json) }
    }

    private val websocketClient = HttpClient {
        install(ContentNegotiation) { json(json) }
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(json)
            pingInterval = 10_000
        }
    }

    private var socketSession: WebSocketSession? = null

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

    public suspend fun updateConfig(pillWeights: PillWeights): Result<PillWeights?> = runCatching {
        postApi<PillWeights>("$url/weight") {
            contentType(ContentType.Application.Json)
            setBody(pillWeights)
        }
    }

    public fun socketConnection(): Flow<Result<PillCount>> = channelFlow {
        socketSession?.close()
        socketSession = websocketClient.webSocketSession(
            method = HttpMethod.Get,
            host = url.host,
            port = url.port,
            path = "/ws"
        )
        async {
            socketSession!!.incoming.consumeEach {
                when (it) {
                    is Frame.Text -> {
                        try {
                            send(Result.success(json.decodeFromString<PillCount>(it.readText())))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    is Frame.Close -> {
                        send(Result.failure(Exception("Nope")))
                        close()
                    }

                    else -> {
                        send(Result.failure(Exception("Nope")))
                        println(it)
                    }
                }
            }
        }.await()
    }
        .retryWhen { cause, attempt ->
            emit(Result.failure(cause))
            println("#$attempt: ${cause.message}")
            if (attempt >= 10 || socketSession?.isActive == true) {
                false
            } else {
                cause is IOException
            }
        }
        .flowOn(Dispatchers.Default)

    public fun pillWeightCalibration(): Flow<PillWeights> = flow {
        withTimeout(5000) {
            while (true) {
                runCatching { getApi<PillWeights>("$url/pillWeight") }.getOrNull()?.let { emit(it) }
                delay(10)
            }
        }
    }

    public suspend fun close() {
        client.close()
        websocketClient.close()
        socketSession?.close()
    }
}
