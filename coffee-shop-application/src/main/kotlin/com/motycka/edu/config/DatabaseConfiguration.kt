package com.motycka.edu.config

import com.motycka.edu.customer.CustomerDAO
import com.motycka.edu.customer.CustomerTable
import com.motycka.edu.menu.MenuItemDAO
import com.motycka.edu.menu.MenuItemTable
import com.motycka.edu.order.OrderItemTable // Import OrderItemTable
import com.motycka.edu.order.OrderTable // Import OrderTable
import com.motycka.edu.user.UserDAO
import com.motycka.edu.user.UserRole
import com.motycka.edu.user.UserTable
import io.ktor.server.application.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabases() {
    Database.connect(
        url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        driver = "org.h2.Driver",
        user = "root",
        password = ""
    )

    transaction {
        SchemaUtils.create(UserTable)
        SchemaUtils.create(MenuItemTable)
        SchemaUtils.create(CustomerTable)
        SchemaUtils.create(OrderTable) // Add OrderTable creation
        SchemaUtils.create(OrderItemTable) // Add OrderItemTable creation

        UserDAO.new {
            username = "admin"
            password = "password" // In a real application, use hashed passwords
            role = UserRole.STAFF
        }
        UserDAO.new {
            username = "staff"
            password = "password" // In a real application, use hashed passwords
            role = UserRole.STAFF
        }
        UserDAO.new {
            username = "customer"
            password = "password" // In a real application, use hashed passwords
            role = UserRole.CUSTOMER
        }

        CustomerDAO.new {
            userId = UserDAO.find { UserTable.username eq "admin" }.firstOrNull()?.id?.value
                ?: error("Admin user not found")
            name = "Administrator"
            discountPercent = 20.0
        }

        CustomerDAO.new {
            userId = UserDAO.find { UserTable.username eq "staff" }.firstOrNull()?.id?.value
                ?: error("Staff user not found")
            name = "Staff"
            discountPercent = 15.0
        }

        CustomerDAO.new {
            userId = UserDAO.find { UserTable.username eq "customer" }.firstOrNull()?.id?.value
                ?: error("Customer user not found")
            name = "Coffee Lover"
            discountPercent = 10.0
        }

        MenuItemDAO.new {
            name = "Espresso"
            description = "Strong coffee brewed by forcing hot water through finely-ground coffee beans."
            price = 2.50
            isDeleted = false
        }
        MenuItemDAO.new {
            name = "Cappuccino"
            description = "Espresso mixed with steamed milk and topped with foamed milk."
            price = 3.00
            isDeleted = false
        }
        MenuItemDAO.new {
            name = "Latte"
            description = "Espresso with steamed milk and a light layer of foam."
            price = 3.50
            isDeleted = false
        }
        MenuItemDAO.new {
            name = "Americano"
            description = "Espresso diluted with hot water."
            price = 2.00
            isDeleted = false
        }
        MenuItemDAO.new {
            name = "Mocha"
            description = "Espresso with steamed milk and chocolate syrup."
            price = 3.75
            isDeleted = false
        }
    }
}

suspend fun <T> suspendTransaction(block: Transaction.() -> T): T =
    newSuspendedTransaction(Dispatchers.IO, statement = block)
