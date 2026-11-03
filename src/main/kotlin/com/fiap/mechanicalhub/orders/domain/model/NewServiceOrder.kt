package com.fiap.mechanicalhub.orders.domain.model

import java.time.Instant
import java.util.UUID

/** Dados de abertura da ordem. */
data class NewServiceOrder(
    val id: UUID,
    val orderNumber: String,
    val customerId: UUID,
    val vehicleId: UUID,
    val createdByUserId: UUID,
    val requestDescription: String,
    val repairIds: List<UUID>,
    val openedAt: Instant,
)
