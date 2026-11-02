package com.fiap.mechanicalhub.orders.infrastructure.http.dto

import java.util.UUID

data class RepairResponse(
    val id: UUID,
    val name: String,
    val description: String?,
)
