package com.motycka.edu.order

import com.motycka.edu.menu.MenuItemResponse
import kotlinx.serialization.Serializable

@Serializable
data class OrderResponse(
    val id: OrderId,
    val menuItems: List<OrderItemResponse>,
    val totalPrice: Double,
    val status: OrderStatus,
    val isPaid: Boolean // Added isPaid
)
