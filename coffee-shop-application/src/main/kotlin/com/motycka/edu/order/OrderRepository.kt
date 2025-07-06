package com.motycka.edu.order

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

interface OrderRepository {

    fun selectAll(): List<OrderDTO>

    fun selectById(id: OrderId): OrderDTO?

    fun create(order: OrderDTO): OrderDTO

    fun update(order: OrderDTO): OrderDTO

}

class OrderRepositoryImpl : OrderRepository {
    
    override fun selectAll(): List<OrderDTO> = transaction {
        OrderDAO.all().map { it.toDTO() }
    }

    override fun selectById(id: OrderId): OrderDTO? = transaction {
        OrderDAO.findById(id)?.toDTO()
    }

    override fun create(order: OrderDTO): OrderDTO = transaction {
        val orderDAO = OrderDAO.new {
            customerId = order.customerId
            totalPrice = order.totalPrice
            status = order.status
            isPaid = order.isPaid
        }
        orderDAO.toDTO()
    }

    override fun update(order: OrderDTO): OrderDTO = transaction {
        val orderDAO = OrderDAO.findById(order.id!!)
            ?: throw IllegalArgumentException("Order not found")
        
        orderDAO.customerId = order.customerId
        orderDAO.totalPrice = order.totalPrice
        orderDAO.status = order.status
        orderDAO.isPaid = order.isPaid
        
        orderDAO.toDTO()
    }
}

