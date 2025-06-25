package com.motycka.edu.order

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object OrderTable : LongIdTable("customer_order") {
    val customerId = long("customer_id")
    val totalPrice = double("total_price") // Added totalPrice column
    val status = enumerationByName("status", 50, OrderStatus::class)
    val isPaid = bool("is_paid").default(false) // Added isPaid column with default false
}

class OrderDAO(id: EntityID<Long>) : LongEntity(id) {
    var customerId by OrderTable.customerId
    var totalPrice by OrderTable.totalPrice // Added totalPrice property
    var status by OrderTable.status
    var isPaid by OrderTable.isPaid // Added isPaid property

    companion object : LongEntityClass<OrderDAO>(OrderTable)

    fun toDTO(): OrderDTO {
        return OrderDTO(
            id = id.value,
            customerId = customerId,
            totalPrice = totalPrice, // Include totalPrice in DTO conversion
            status = status,
            isPaid = isPaid // Include isPaid in DTO conversion
        )
    }
}
