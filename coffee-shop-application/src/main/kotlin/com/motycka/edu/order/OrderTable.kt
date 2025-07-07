package com.motycka.edu.order

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object OrderTable : LongIdTable("orders") {
    val customerId = long("customer_id")
    val totalPrice = double("total_price")
    val status = enumerationByName("status", 20, OrderStatus::class)
    val isPaid = bool("is_paid")
}

class OrderDAO(id: EntityID<Long>) : LongEntity(id) {
    var customerId by OrderTable.customerId
    var totalPrice by OrderTable.totalPrice
    var status by OrderTable.status
    var isPaid by OrderTable.isPaid

    companion object : LongEntityClass<OrderDAO>(OrderTable)

    fun toDTO(): OrderDTO {
        return OrderDTO(
            id = id.value,
            customerId = customerId,
            totalPrice = totalPrice,
            status = status,
            isPaid = isPaid
        )
    }
}
