package com.motycka.edu.order

import com.motycka.edu.customer.CustomerDTO
import com.motycka.edu.menu.MenuItemDTO
import com.motycka.edu.security.IdentityDTO
import com.motycka.edu.user.UserRole
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking

class OrderServiceLogicTest : FunSpec({
    
    test("PriceCalculator should apply discount correctly") {
        val menuItems = listOf(
            MenuItemDTO(1, "Espresso", "Strong coffee", 2.50, false),
            MenuItemDTO(2, "Cappuccino", "Espresso with milk", 3.00, false)
        )
        
        val orderItems = listOf(
            OrderItemDTO(null, 0, 1, 2), // 2 espressos
            OrderItemDTO(null, 0, 2, 1)  // 1 cappuccino
        )
        
        // Calculate without discount
        val priceWithoutDiscount = PriceCalculator.calculatePrice(menuItems, 0.0, orderItems)
        priceWithoutDiscount.shouldBe(8.0) // (2.50 * 2) + (3.00 * 1) = 8.0
        
        // Calculate with 10% discount
        val priceWithDiscount = PriceCalculator.calculatePrice(menuItems, 10.0, orderItems)
        priceWithDiscount.shouldBe(7.2) // 8.0 * 0.9 = 7.2
        
        // Calculate with 20% discount
        val priceWith20Discount = PriceCalculator.calculatePrice(menuItems, 20.0, orderItems)
        priceWith20Discount.shouldBe(6.4) // 8.0 * 0.8 = 6.4
    }
    
    test("OrderRequest validation should work correctly") {
        val validRequest = OrderRequest(
            customerId = 1L,
            items = listOf(
                OrderItemRequest(menuItemId = 1, quantity = 2)
            )
        )
        
        validRequest.customerId.shouldNotBe(null)
        validRequest.items.shouldNotBe(null)
        validRequest.items!!.size.shouldBe(1)
        validRequest.items!![0].menuItemId.shouldBe(1L)
        validRequest.items!![0].quantity.shouldBe(2)
    }
    
    test("OrderResponse structure should match API specification") {
        val menuItemResponse = com.motycka.edu.menu.MenuItemResponse(1, "Espresso", "Strong coffee", 2.50)
        val orderItemResponse = OrderItemResponse(menuItemResponse, 2)
        
        val orderResponse = OrderResponse(
            id = 1L,
            customerId = 1L,
            menuItems = listOf(orderItemResponse),
            totalPrice = 5.0,
            status = OrderStatus.PENDING,
            isPaid = false
        )
        
        orderResponse.id.shouldBe(1L)
        orderResponse.customerId.shouldBe(1L)
        orderResponse.menuItems.size.shouldBe(1)
        orderResponse.menuItems[0].menuItem.id.shouldBe(1)
        orderResponse.menuItems[0].menuItem.name.shouldBe("Espresso")
        orderResponse.menuItems[0].quantity.shouldBe(2)
        orderResponse.totalPrice.shouldBe(5.0)
        orderResponse.status.shouldBe(OrderStatus.PENDING)
        orderResponse.isPaid.shouldBe(false)
    }
    
    test("OrderStatus enum should have correct values") {
        OrderStatus.values().size.shouldBe(2)
        OrderStatus.PENDING.shouldNotBe(OrderStatus.COMPLETED)
        OrderStatus.valueOf("PENDING").shouldBe(OrderStatus.PENDING)
        OrderStatus.valueOf("COMPLETED").shouldBe(OrderStatus.COMPLETED)
    }
    
    test("IdentityDTO should contain correct user information") {
        val identity = IdentityDTO(
            userId = 1L,
            customerId = 1L,
            role = UserRole.CUSTOMER
        )
        
        identity.userId.shouldBe(1L)
        identity.customerId.shouldBe(1L)
        identity.role.shouldBe(UserRole.CUSTOMER)
    }
}) 