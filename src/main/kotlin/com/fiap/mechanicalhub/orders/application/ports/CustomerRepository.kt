package com.fiap.mechanicalhub.orders.application.ports

import com.fiap.mechanicalhub.orders.domain.model.Customer
import java.util.UUID

interface CustomerRepository {
    fun findById(id: UUID): Customer?
}
