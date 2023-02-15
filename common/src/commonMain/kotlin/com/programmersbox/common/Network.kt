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

    suspend inline fun <reified T> getApi(
        url: String,
        noinline headers: HeadersBuilder.() -> Unit = {}
    ): T? {
        val response: HttpResponse = client.get(url) { headers(headers) }
        return response.body<T>()
    }

    suspend inline fun <reified T> postApi(
        url: String,
        noinline builder: HttpRequestBuilder.() -> Unit = {}
    ): T? {
        val response: HttpResponse = client.post(url, builder)
        return response.body<T>()
    }

    suspend fun updateConfig(pillWeights: PillWeights) = runCatching {
        postApi<PillWeights>("http://0.0.0.0:8080/weight") { setBody(pillWeights) }
    }

    suspend fun socketConnection(): Flow<PillCount> {
        val updateFlow = MutableSharedFlow<PillCount>()
        client.ws(method = HttpMethod.Get, host = "0.0.0.0", port = 8080, path = "/ws") {
            incoming
                .consumeAsFlow()
                .filterIsInstance<Frame.Text>()
                .map { it.readText() }
                .mapNotNull { text ->
                    println(text)
                    try {
                        json.decodeFromString<PillCount>(text)
                    } catch (e: Exception) {
                        null
                    }
                }
                .onEach(updateFlow::emit)
                .collect()
        }
        return updateFlow
    }
}
