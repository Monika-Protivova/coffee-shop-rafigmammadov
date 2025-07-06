package com.motycka.edu.order

import com.motycka.edu.security.getUserIdentity
import com.motycka.edu.user.UserRole
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*

private val logger = KotlinLogging.logger {}

private const val ORDER_NOT_FOUND = "Order not found"
private const val INVALID_ID = "Invalid ID format"
private const val UNAUTHORIZED_ROLE = "Access denied: STAFF or CUSTOMER role required"

fun Route.orderRoutes(
    orderService: OrderService,
    basePath: String
) {
    route("$basePath/orders") {
        
        get {
            try {
                val userIdentity = this.getUserIdentity()
                if (userIdentity.role != UserRole.STAFF && userIdentity.role != UserRole.CUSTOMER) {
                    return@get call.respond(HttpStatusCode.Forbidden, mapOf("error" to UNAUTHORIZED_ROLE))
                }
                
                val orders = orderService.getAllOrders()
                call.respond(HttpStatusCode.OK, orders)
            } catch (e: Exception) {
                logger.error(e) { "Error retrieving orders" }
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Internal server error"))
            }
        }
        
        get("/{id}") {
            try {
                val userIdentity = this.getUserIdentity()
                if (userIdentity.role != UserRole.STAFF && userIdentity.role != UserRole.CUSTOMER) {
                    return@get call.respond(HttpStatusCode.Forbidden, mapOf("error" to UNAUTHORIZED_ROLE))
                }
                
                val idString = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing order ID"))
                val id = idString.toLongOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to INVALID_ID))
                
                val order = orderService.getOrderById(id)
                if (order == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to ORDER_NOT_FOUND))
                } else {
                    call.respond(HttpStatusCode.OK, order)
                }
            } catch (e: Exception) {
                logger.error(e) { "Error retrieving order" }
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Internal server error"))
            }
        }
        
        post {
            try {
                val userIdentity = this.getUserIdentity()
                if (userIdentity.role != UserRole.STAFF && userIdentity.role != UserRole.CUSTOMER) {
                    return@post call.respond(HttpStatusCode.Forbidden, mapOf("error" to UNAUTHORIZED_ROLE))
                }
                
                val request = call.receive<OrderRequest>()
                
                // Validate request
                if (request.items.isNullOrEmpty()) {
                    return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Items cannot be null or empty"))
                }
                
                val order = orderService.createOrder(request, userIdentity)
                call.respond(HttpStatusCode.Created, order)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            } catch (e: Exception) {
                logger.error(e) { "Error creating order" }
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Internal server error"))
            }
        }
        
        put("/{id}") {
            try {
                val userIdentity = this.getUserIdentity()
                if (userIdentity.role != UserRole.STAFF && userIdentity.role != UserRole.CUSTOMER) {
                    return@put call.respond(HttpStatusCode.Forbidden, mapOf("error" to UNAUTHORIZED_ROLE))
                }
                
                val idString = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing order ID"))
                val id = idString.toLongOrNull() ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to INVALID_ID))
                
                val request = call.receive<OrderUpdateRequest>()
                val order = orderService.updateOrder(id, request)
                call.respond(HttpStatusCode.OK, order)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            } catch (e: Exception) {
                logger.error(e) { "Error updating order" }
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Internal server error"))
            }
        }
    }
}
