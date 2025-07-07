package com.motycka.edu.order

import com.motycka.edu.customer.CustomerRepository
import com.motycka.edu.menu.MenuItemDTO
import com.motycka.edu.menu.MenuItemResponse
import com.motycka.edu.menu.MenuRepository
import com.motycka.edu.security.IdentityDTO

class OrderService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val menuRepository: MenuRepository,
    private val customerRepository: CustomerRepository
) {
    
    suspend fun getAllOrders(): List<OrderResponse> {
        val orders = orderRepository.selectAll()
        return orders.map { orderDTO ->
            val orderItems = orderItemRepository.selectByOrderId(orderDTO.id!!)
            val menuItems = menuRepository.selectAll()
            // Get customer discount for each order
            val customer = customerRepository.selectById(orderDTO.customerId)
            val discountPercent = customer?.discountPercent ?: 0.0
            createOrderResponse(orderDTO, orderItems, menuItems, discountPercent)
        }
    }
    
    suspend fun getOrderById(id: OrderId): OrderResponse? {
        val orderDTO = orderRepository.selectById(id) ?: return null
        val orderItems = orderItemRepository.selectByOrderId(id)
        val menuItems = menuRepository.selectAll()
        // Get customer discount for the order
        val customer = customerRepository.selectById(orderDTO.customerId)
        val discountPercent = customer?.discountPercent ?: 0.0
        return createOrderResponse(orderDTO, orderItems, menuItems, discountPercent)
    }
    
    suspend fun createOrder(request: OrderRequest, userIdentity: IdentityDTO): OrderResponse {
        // Get customer discount based on the authenticated user
        val customer = customerRepository.selectByUserId(userIdentity.userId)
            ?: throw IllegalArgumentException("Customer not found")
        
        // Use provided customerId or fall back to authenticated user's customer ID
        val orderCustomerId = request.customerId ?: userIdentity.customerId
        
        // Validate items
        val items = request.items ?: throw IllegalArgumentException("Items cannot be null")
        if (items.isEmpty()) {
            throw IllegalArgumentException("Items cannot be empty")
        }
        
        // Get menu items for price calculation
        val menuItems = menuRepository.selectAll()
        
        // Convert request items to DTOs
        val orderItemDTOs = items.map { itemRequest ->
            OrderItemDTO(
                id = null,
                orderId = 0, // Will be set after order creation
                menuItemId = itemRequest.menuItemId,
                quantity = itemRequest.quantity
            )
        }
        
        // Calculate total price with discount from the customer making the request
        val totalPrice = PriceCalculator.calculatePrice(menuItems, customer.discountPercent, orderItemDTOs)
        
        // Create order
        val orderDTO = OrderDTO(
            id = null,
            customerId = orderCustomerId,
            totalPrice = totalPrice,
            status = OrderStatus.PENDING,
            isPaid = false
        )
        
        val createdOrder = orderRepository.create(orderDTO)
        
        // Create order items with the correct order ID
        val orderItemsWithOrderId = orderItemDTOs.map { it.copy(orderId = createdOrder.id!!) }
        orderItemRepository.createOrderItems(orderItemsWithOrderId)
        
        // Return response
        val finalOrderItems = orderItemRepository.selectByOrderId(createdOrder.id!!)
        return createOrderResponse(createdOrder, finalOrderItems, menuItems, customer.discountPercent)
    }
    
    suspend fun updateOrder(id: OrderId, request: OrderUpdateRequest): OrderResponse {
        val existingOrder = orderRepository.selectById(id)
            ?: throw IllegalArgumentException("Order not found")
        
        // Validate status
        val newStatus = request.status ?: throw IllegalArgumentException("Status cannot be null")
        
        val updatedOrder = existingOrder.copy(status = newStatus)
        val savedOrder = orderRepository.update(updatedOrder)
        
        val orderItems = orderItemRepository.selectByOrderId(id)
        val menuItems = menuRepository.selectAll()
        // Get customer discount for the order
        val customer = customerRepository.selectById(savedOrder.customerId)
        val discountPercent = customer?.discountPercent ?: 0.0
        return createOrderResponse(savedOrder, orderItems, menuItems, discountPercent)
    }
    
    private fun createOrderResponse(
        orderDTO: OrderDTO,
        orderItems: List<OrderItemDTO>,
        menuItems: List<MenuItemDTO>,
        discountPercent: Double
    ): OrderResponse {
        val orderItemResponses = orderItems.map { orderItem ->
            val menuItem = menuItems.find { it.id == orderItem.menuItemId }
                ?: throw IllegalArgumentException("Menu item not found: ${orderItem.menuItemId}")
            OrderItemResponse(
                menuItem = MenuItemResponse(menuItem.id!!, menuItem.name, menuItem.description, menuItem.price),
                quantity = orderItem.quantity
            )
        }
        
        // The total price should already include the discount from when the order was created
        // We don't need to recalculate it here as it would double-apply the discount
        return OrderResponse(
            id = orderDTO.id!!,
            customerId = orderDTO.customerId,
            menuItems = orderItemResponses,
            totalPrice = orderDTO.totalPrice,
            status = orderDTO.status,
            isPaid = orderDTO.isPaid
        )
    }
} 