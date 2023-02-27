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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json

internal class Network(
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
        postApi<PillWeights>("$url/weight") {
            contentType(ContentType.Application.Json)
            setBody(pillWeights)
        }
    }

    fun socketConnection(): Flow<Result<PillCount>> = flow {
        while (client.isActive) {
            runCatching { getApi<PillCount>("$url/currentCount") }
                .onSuccess { it?.let { p -> emit(Result.success(p)) } }
                .onFailure { emit(Result.failure(it)) }
            delay(1000)
        }
    }

    fun pillWeightCalibration(): Flow<PillWeights> = flow {
        withTimeout(5000) {
            while (true) {
                runCatching { getApi<PillWeights>("$url/pillWeight") }.getOrNull()?.let { emit(it) }
                delay(10)
            }
        }
    }

    fun close() {
        client.close()
        websocketClient.close()
    }
}
