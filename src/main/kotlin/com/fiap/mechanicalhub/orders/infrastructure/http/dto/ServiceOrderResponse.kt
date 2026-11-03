package com.fiap.mechanicalhub.orders.infrastructure.http.dto

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class ServiceOrderResponse(
    val id: UUID,
    val orderNumber: String,
    val status: StatusResponse,
    val customerId: UUID,
    val vehicleId: UUID,
    val createdByUserId: UUID,
    val responsibleUserId: UUID?,
    val requestDescription: String,
    val budget: BigDecimal?,
    val hasStockPending: Boolean,
    val repairs: List<OrderTaskResponse>,
    val openedAt: Instant,
    val completedAt: Instant?,
    val deliveredAt: Instant?,
)
