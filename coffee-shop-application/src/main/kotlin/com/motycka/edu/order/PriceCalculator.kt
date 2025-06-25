package com.motycka.edu.order

import com.motycka.edu.menu.MenuItemDTO

object PriceCalculator {

    fun calculatePrice(menuItems: List<MenuItemDTO>, discountInPercent: Double, orderItems: List<OrderItemDTO> = emptyList()): Double {
        var originalPrice = 0.0

        // Create a map for quick lookup of MenuItemDTO by its ID
        val menuItemMap = menuItems.associateBy { it.id }

        for (orderItem in orderItems) {
            val menuItem = menuItemMap[orderItem.menuItemId]
            if (menuItem != null) {
                originalPrice += menuItem.price * orderItem.quantity
            } else {
                // Handle case where menu item is not found (e.g., log a warning or throw an exception)
                // For this task, we'll assume all menu items exist in the provided list.
            }
        }

        // Apply discount
        val finalPrice = originalPrice * (1 - discountInPercent / 100)
        return finalPrice
    }
}
