package com.fiap.mechanicalhub.orders.infrastructure.http.dto

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class CustomerOrderResponse(
    val orderId: UUID,
    val orderNumber: String,
    val customerName: String,
    val vehicleLicensePlate: String,
    val vehicleModel: String,
    val vehicleBrand: String,
    val status: StatusResponse,
    val budget: BigDecimal?,
    val services: List<String>,
    val openedAt: Instant,
    val completedAt: Instant?,
    val deliveredAt: Instant?,
)
