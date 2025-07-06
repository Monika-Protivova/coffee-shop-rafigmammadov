package com.motycka.edu.customer

import com.motycka.edu.user.UserId
import org.jetbrains.exposed.sql.transactions.transaction

class CustomerRepositoryImpl : CustomerRepository {

    override fun selectCustomer(userId: UserId): CustomerDTO? = transaction {
        CustomerDAO.find { CustomerTable.userId eq userId }.firstOrNull()?.toDTO()
    }
    
    override fun selectByUserId(userId: UserId): CustomerDTO? = transaction {
        CustomerDAO.find { CustomerTable.userId eq userId }.firstOrNull()?.toDTO()
    }
    
    override fun selectById(customerId: CustomerId): CustomerDTO? = transaction {
        CustomerDAO.findById(customerId)?.toDTO()
    }
}
