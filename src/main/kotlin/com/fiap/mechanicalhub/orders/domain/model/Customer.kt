package com.fiap.mechanicalhub.orders.domain.model

import java.util.UUID

data class Customer(
    val id: UUID,
    val name: String,
    val documentNumber: String,
    val telephone: String,
    val email: String,
)
