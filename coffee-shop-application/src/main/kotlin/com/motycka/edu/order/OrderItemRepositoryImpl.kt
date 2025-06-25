package com.motycka.edu.order

import com.motycka.edu.config.suspendTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class OrderItemRepositoryImpl : OrderItemRepository {

    override fun selectByOrderId(orderId: OrderId): List<OrderItemDTO> = transaction {
        OrderItemDAO.find { OrderItemTable.orderId eq orderId }.map { it.toDTO() }
    }

    override fun createOrderItems(orderItems: List<OrderItemDTO>) = transaction {
        orderItems.forEach { item ->
            OrderItemDAO.new {
                orderId = item.orderId
                menuItemId = item.menuItemId
                quantity = item.quantity
            }
        }
    }
}
