package com.motycka.edu.order

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

interface OrderItemRepository {
    fun selectByOrderId(orderId: OrderId): List<OrderItemDTO>
    fun createOrderItems(orderItems: List<OrderItemDTO>)
}

class OrderItemRepositoryImpl : OrderItemRepository {
    
    override fun selectByOrderId(orderId: OrderId): List<OrderItemDTO> = transaction {
        OrderItemDAO.find { OrderItemTable.orderId eq orderId }.map { it.toDTO() }
    }

    override fun createOrderItems(orderItems: List<OrderItemDTO>) = transaction {
        orderItems.forEach { orderItem ->
            OrderItemDAO.new {
                this.orderId = orderItem.orderId
                this.menuItemId = orderItem.menuItemId
                this.quantity = orderItem.quantity
            }
        }
    }
}
