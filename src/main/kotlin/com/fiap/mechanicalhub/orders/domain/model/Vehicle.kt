package com.fiap.mechanicalhub.orders.domain.model

import java.util.UUID

data class Vehicle(
    val id: UUID,
    val customerId: UUID,
    val licensePlate: String,
    val brand: String,
    val model: String,
)
