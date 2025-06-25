package com.motycka.edu.order

import kotlinx.serialization.Serializable

@Serializable
data class OrderRequest(
    val customerId: Long?, // Added customerId as nullable
    val items: List<OrderItemRequest>
)
