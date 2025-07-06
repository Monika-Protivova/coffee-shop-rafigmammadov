package com.motycka.edu

import com.motycka.edu.config.AUTH_JWT
import com.motycka.edu.config.configureDatabases
import com.motycka.edu.config.configureJWT
import com.motycka.edu.customer.CustomerRepositoryImpl
import com.motycka.edu.customer.InternalCustomerService
import com.motycka.edu.menu.MenuRepositoryImpl
import com.motycka.edu.menu.MenuService
import com.motycka.edu.menu.menuRoutes
import com.motycka.edu.order.OrderRepositoryImpl
import com.motycka.edu.order.OrderItemRepositoryImpl
import com.motycka.edu.order.OrderService
import com.motycka.edu.order.orderRoutes
import com.motycka.edu.security.AuthenticationService
import com.motycka.edu.security.JwtService
import com.motycka.edu.security.loginRoutes
import com.motycka.edu.user.UserRepositoryImpl
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

private val logger = KotlinLogging.logger {}

const val API_PATH = "/api/v1"

fun Application.module() {
    logger.info { "Starting application with configuration" }

    // Configure the database
    configureDatabases()

    val menuRepository = MenuRepositoryImpl()
    val menuService = MenuService(menuRepository = menuRepository)
    val jwtGenerator = JwtService(config = this@module.environment.config)
    val userRepository = UserRepositoryImpl()
    val customerRepository = CustomerRepositoryImpl()
    val authenticationService = AuthenticationService(
        userRepository = userRepository,
        internalCustomerService = InternalCustomerService(customerRepository = customerRepository),
        jwtService = jwtGenerator
    )
    
    val orderRepository = OrderRepositoryImpl()
    val orderItemRepository = OrderItemRepositoryImpl()
    val orderService = OrderService(
        orderRepository = orderRepository,
        orderItemRepository = orderItemRepository,
        menuRepository = menuRepository,
        customerRepository = customerRepository
    )

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
        })
    }

    install(Authentication) {
        configureJWT(this@module.environment.config)
    }

    routing {
        loginRoutes(authenticationService, API_PATH)

        authenticate(AUTH_JWT) {
            menuRoutes(menuService, API_PATH)
            orderRoutes(orderService, API_PATH)
        }
    }
}

fun main() {
    // Use the KTOR_CONFIG_FILE environment variable if set, otherwise use application.yaml
    val configFile = System.getenv("KTOR_CONFIG_FILE") ?: "application.yaml"
    
    // Create a simple embedded server with configuration from the specified config file
    val applicationConfig = io.ktor.server.config.ApplicationConfig(configFile)
    val ktorConfig = applicationConfig.config("ktor.deployment")
    val port = ktorConfig.property("port").getString().toInt()
    val host = ktorConfig.propertyOrNull("host")?.getString() ?: "0.0.0.0"

    embeddedServer(Netty, port = port, host = host) {
        module()
    }.start(wait = true)
}
