package com.motycka.edu.order

import com.motycka.edu.config.suspendTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class OrderRepositoryImpl : OrderRepository {

    override fun selectAll(): List<OrderDTO> = transaction {
        OrderDAO.all().map { it.toDTO() }
    }

    override fun selectById(id: OrderId): OrderDTO? = transaction {
        OrderDAO.findById(id)?.toDTO()
    }

    override fun create(order: OrderDTO): OrderDTO = transaction {
        OrderDAO.new {
            customerId = order.customerId
            totalPrice = order.totalPrice // Set totalPrice
            status = order.status
            isPaid = order.isPaid // Set isPaid
        }.toDTO()
    }

    override fun update(order: OrderDTO): OrderDTO = transaction {
        val orderDAO = OrderDAO.findById(order.id!!) ?: throw IllegalArgumentException("Order not found with ID: ${order.id}")
        orderDAO.customerId = order.customerId
        orderDAO.totalPrice = order.totalPrice // Update totalPrice
        orderDAO.status = order.status
        orderDAO.isPaid = order.isPaid // Update isPaid
        orderDAO.toDTO()
    }
}
