package com.motycka.edu.order


data class OrderDTO(
    val id: OrderId?,
    val customerId: Long,
    val totalPrice: Double, // Added totalPrice
    val status: OrderStatus,
    val isPaid: Boolean // Added isPaid
)
