package com.motycka.edu.order

import com.motycka.edu.customer.InternalCustomerService
import com.motycka.edu.error.UnauthorizedException
import com.motycka.edu.menu.InternalMenuService
import com.motycka.edu.menu.MenuRepository
import com.motycka.edu.menu.MenuItemDTO
import com.motycka.edu.menu.MenuItemResponse
import com.motycka.edu.security.IdentityDTO
import com.motycka.edu.user.UserRole
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class OrderService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository = OrderItemRepositoryImpl(),
    private val menuRepository: MenuRepository,
    private val customerService: InternalCustomerService
) {

    suspend fun getAllOrders(identity: IdentityDTO): List<OrderResponse> {
        logger.info { "Getting all orders for user: ${identity.userId} with role: ${identity.role}" }
        val orders = orderRepository.selectAll()
        return orders.mapNotNull { orderDTO ->
            // Only return orders relevant to the customer if the user is a CUSTOMER
            if (identity.role == UserRole.CUSTOMER && orderDTO.customerId != identity.customerId) {
                null
            } else {
                mapOrderDTOToResponse(orderDTO)
            }
        }
    }

    suspend fun getOrderById(identity: IdentityDTO, id: OrderId): OrderResponse? {
        logger.info { "Getting order with id: $id for user: ${identity.userId} with role: ${identity.role}" }
        val orderDTO = orderRepository.selectById(id) ?: return null

        // Authorization check: Customer can only view their own orders
        if (identity.role == UserRole.CUSTOMER && orderDTO.customerId != identity.customerId) {
            throw UnauthorizedException("Customer is not authorized to view this order.")
        }

        return mapOrderDTOToResponse(orderDTO)
    }

    suspend fun createOrder(identity: IdentityDTO, request: OrderRequest): OrderResponse {
        logger.info { "Creating order for user: ${identity.userId} with role: ${identity.role}" }

        // Validate request
        if (request.items.isEmpty()) {
            throw IllegalArgumentException("Order must contain at least one item.")
        }

        val customerIdForOrder = when (identity.role) {
            UserRole.CUSTOMER -> identity.customerId
            UserRole.STAFF -> {
                // Staff can specify customerId in the request, otherwise use their own customerId
                request.customerId ?: identity.customerId
            }
        }

        // Ensure the customer exists and get their discount
        val customer = customerService.getCustomer(customerIdForOrder)
            ?: throw IllegalArgumentException("Customer with ID $customerIdForOrder not found.")
        val customerDiscount = customer.discountPercent

        // Retrieve menu items for price calculation
        val menuItemIds = request.items.map { it.menuItemId }.toSet()
        val menuItems = menuRepository.selectMenuItems(filter = null, ids = menuItemIds).toList()

        // Calculate total price based on initial items and discount
        val initialOrderItems = request.items.map { itemRequest ->
            OrderItemDTO(
                id = null, // ID will be set after creation
                orderId = 0, // Placeholder, will be updated after order creation
                menuItemId = itemRequest.menuItemId,
                quantity = itemRequest.quantity
            )
        }
        val totalPrice = PriceCalculator.calculatePrice(menuItems, customerDiscount, initialOrderItems)

        // Create the main order entry
        val newOrderDTO = OrderDTO(
            id = null,
            customerId = customerIdForOrder,
            totalPrice = totalPrice, // Set totalPrice
            status = OrderStatus.PENDING, // New orders are pending by default
            isPaid = false // New orders are not paid by default
        )
        val createdOrderDTO = orderRepository.create(newOrderDTO)

        // Prepare and create order items with the actual orderId
        val orderItemsToCreate = request.items.map { itemRequest ->
            OrderItemDTO(
                id = null,
                orderId = createdOrderDTO.id!!, // Use the ID of the newly created order
                menuItemId = itemRequest.menuItemId,
                quantity = itemRequest.quantity
            )
        }
        orderItemRepository.createOrderItems(orderItemsToCreate)

        return mapOrderDTOToResponse(createdOrderDTO, orderItemsToCreate, menuItems, totalPrice)
    }

    suspend fun updateOrder(identity: IdentityDTO, id: OrderId, request: OrderUpdateRequest): OrderResponse? {
        logger.info { "Updating order with id: $id for user: ${identity.userId} with role: ${identity.role}" }

        val existingOrderDTO = orderRepository.selectById(id) ?: return null

        // Authorization check: Only staff can update order status
        if (identity.role != UserRole.STAFF) {
            throw UnauthorizedException("Only staff members are authorized to update order status.")
        }

        // Update the order status
        val updatedOrderDTO = existingOrderDTO.copy(status = request.status)
        orderRepository.update(updatedOrderDTO)

        return mapOrderDTOToResponse(updatedOrderDTO)
    }

    private suspend fun mapOrderDTOToResponse(
        orderDTO: OrderDTO,
        orderItemsDTO: List<OrderItemDTO>? = null,
        menuItemsDTO: List<MenuItemDTO>? = null,
        calculatedTotalPrice: Double? = null
    ): OrderResponse {
        val items = orderItemsDTO ?: orderItemRepository.selectByOrderId(orderDTO.id!!)
        val menuItems = menuItemsDTO ?: menuRepository.selectMenuItems(null, items.map { it.menuItemId }.toSet()).toList()

        val customerDiscount = customerService.getDiscountPercent(orderDTO.customerId)

        val finalTotalPrice = calculatedTotalPrice ?: PriceCalculator.calculatePrice(menuItems, customerDiscount, items)

        val orderItemResponses = items.map { orderItem ->
            val menuItem = menuItems.find { it.id == orderItem.menuItemId }
            OrderItemResponse(
                menuItem = MenuItemResponse(
                    id = menuItem?.id ?: 0,
                    name = menuItem?.name ?: "Unknown",
                    description = menuItem?.description ?: "Unknown",
                    price = menuItem?.price ?: 0.0
                ),
                quantity = orderItem.quantity
            )
        }

        return OrderResponse(
            id = orderDTO.id!!,
            menuItems = orderItemResponses,
            totalPrice = finalTotalPrice,
            status = orderDTO.status,
            isPaid = orderDTO.isPaid // Include isPaid in the response
        )
    }
}
