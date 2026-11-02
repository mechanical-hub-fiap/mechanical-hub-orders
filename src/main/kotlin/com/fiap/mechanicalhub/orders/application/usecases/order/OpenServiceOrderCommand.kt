package com.fiap.mechanicalhub.orders.application.usecases.order

import java.util.UUID

data class OpenServiceOrderCommand(
    val customerId: UUID,
    val vehicleId: UUID,
    val repairIds: List<UUID>,
    val requestDescription: String,
    val userId: UUID,
)
