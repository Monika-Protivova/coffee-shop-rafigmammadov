package com.motycka.edu

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FreeSpec
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.DefaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.call.body
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

/**
 * Base class for integration tests.
 * This class provides HTTP clients for making requests to the application.
 * The application is started in a Docker container for all tests.
 */
abstract class IntegrationTestBase : FreeSpec() {

    override fun isolationMode() = IsolationMode.InstancePerTest

    companion object {
        private val container = ApplicationContainer().apply {
            start()
        }
    }

    protected val baseUrl by lazy { container.getBaseUrl() }

    protected val credentials = "admin:password"
    protected val encodedCredentials = java.util.Base64.getEncoder().encodeToString(credentials.toByteArray())
    protected val authHeader = "Basic $encodedCredentials"

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    protected fun runTest(block: suspend (client: HttpClient) -> Unit) {
        runBlocking {
            block(client)
        }
    }

    protected fun getJwtToken(username: String, password: String): String = runBlocking {
        val response = client.post("$baseUrl/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginTestRequest(username, password))
        }
        response.body<LoginTestResponse>().token
    }

    protected fun runAuthenticatedTest(
        username: String = "admin",
        password: String = "password",
        block: suspend (client: HttpClient) -> Unit
    ) {
        runBlocking {
            val token = getJwtToken(username, password)
            val authClient = HttpClient(CIO) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
                // Set Authorization header for all requests
                expectSuccess = false
                install(DefaultRequest) {
                    headers.append(HttpHeaders.Authorization, "Bearer $token")
                }
            }
            block(authClient)
        }
    }
}
