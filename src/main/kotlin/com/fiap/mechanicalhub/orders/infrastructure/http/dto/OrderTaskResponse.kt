package com.fiap.mechanicalhub.orders.infrastructure.http.dto

import java.time.Instant
import java.util.UUID

data class OrderTaskResponse(
    val repairId: UUID,
    val name: String?,
    val status: StatusResponse,
    val startedAt: Instant?,
    val finishedAt: Instant?,
)
