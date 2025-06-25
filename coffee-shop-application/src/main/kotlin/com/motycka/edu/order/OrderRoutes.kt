package com.motycka.edu.order

import com.motycka.edu.error.UnauthorizedException
import com.motycka.edu.security.getUserIdentity
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private val logger = KotlinLogging.logger {}

private const val ORDER_NOT_FOUND = "Order not found"
private const val INVALID_ID = "Invalid ID format"

fun Route.orderRoutes(
    orderService: OrderService, // Replaced Any with OrderService
    basePath: String
) {
    route("$basePath/orders") {

        // GET /orders
        get {
            logger.info { "GET request received for all orders" }
            val orders = orderService.getAllOrders(getUserIdentity())
            logger.debug { "Responding with ${orders.size} orders" }
            call.respond(orders)
        }

        // GET /orders/{id}
        get("/{id}") {
            val idParam = call.parameters["id"]
            logger.info { "GET request received for order with id: $idParam" }

            val id = idParam?.toLongOrNull() ?: run {
                logger.warn { "Invalid ID format: $idParam" }
                call.respond(HttpStatusCode.BadRequest, INVALID_ID)
                return@get
            }

            val order = orderService.getOrderById(getUserIdentity(), id)

            if (order != null) {
                logger.debug { "Responding with order: ${order.id}" }
                call.respond(order)
            } else {
                logger.warn { "Order with id: $id not found" }
                call.respond(HttpStatusCode.NotFound, ORDER_NOT_FOUND)
            }
        }

        // POST /orders
        post {
            logger.info { "POST request received to create a new order" }
            try {
                val request = call.receive<OrderRequest>()
                logger.debug { "Creating order for customer: ${getUserIdentity().customerId}" }

                val createdOrder = orderService.createOrder(
                    identity = getUserIdentity(),
                    request = request
                )

                logger.info { "Order created successfully with id: ${createdOrder.id}" }
                call.respond(HttpStatusCode.Created, createdOrder)
            } catch (e: IllegalArgumentException) {
                logger.warn { "Bad request for order creation: ${e.message}" }
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
            } catch (e: UnauthorizedException) {
                logger.warn { "Unauthorized attempt to create order: ${e.message}" }
                call.respond(HttpStatusCode.Forbidden, e.message ?: "Forbidden")
            } catch (e: Exception) {
                logger.error { "Error creating order: ${e.message}" }
                call.respond(HttpStatusCode.InternalServerError, "Error creating order: ${e.message}")
            }
        }

        // PUT /orders/{id}
        put("/{id}") {
            val idParam = call.parameters["id"]
            logger.info { "PUT request received to update order with id: $idParam" }

            val id = idParam?.toLongOrNull() ?: run {
                logger.warn { "Invalid ID format: $idParam" }
                call.respond(HttpStatusCode.BadRequest, INVALID_ID)
                return@put
            }

            val identity = getUserIdentity()
            logger.debug { "Request from user: ${identity.userId}" }

            try {
                val request = call.receive<OrderUpdateRequest>()
                logger.debug { "Updating order status to: ${request.status}" }

                val updatedOrder = orderService.updateOrder(identity, id, request)

                if (updatedOrder != null) {
                    logger.info { "Order updated successfully with id: $id" }
                    call.respond(updatedOrder)
                } else {
                    logger.warn { "Order with id: $id not found for update" }
                    call.respond(HttpStatusCode.NotFound, ORDER_NOT_FOUND)
                }
            } catch (e: IllegalArgumentException) {
                logger.warn { "Bad request for order update: ${e.message}" }
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
            } catch (e: UnauthorizedException) {
                logger.warn { "Unauthorized attempt to update order: ${e.message}" }
                call.respond(HttpStatusCode.Forbidden, e.message ?: "Forbidden")
            } catch (e: Exception) {
                logger.error { "Error updating order: ${e.message}" }
                call.respond(HttpStatusCode.InternalServerError, "Error updating order: ${e.message}")
            }
        }
    }
}
