package com.fiap.mechanicalhub.orders.application.ports

import com.fiap.mechanicalhub.orders.domain.enums.OrderStatusEnum
import java.util.UUID

data class ServiceOrderFilter(
    val status: OrderStatusEnum? = null,
    val customerId: UUID? = null,
    val responsibleUserId: UUID? = null,
)
