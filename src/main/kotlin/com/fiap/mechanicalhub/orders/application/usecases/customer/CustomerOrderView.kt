package com.fiap.mechanicalhub.orders.application.usecases.customer

import com.fiap.mechanicalhub.orders.domain.enums.OrderStatusEnum
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** Resumo da ordem exibido ao cliente (mesmos campos do monolito). */
data class CustomerOrderView(
    val orderId: UUID,
    val orderNumber: String,
    val customerName: String,
    val vehicleLicensePlate: String,
    val vehicleBrand: String,
    val vehicleModel: String,
    val status: OrderStatusEnum,
    val budget: BigDecimal?,
    val services: List<String>,
    val openedAt: Instant,
    val completedAt: Instant?,
    val deliveredAt: Instant?,
)
