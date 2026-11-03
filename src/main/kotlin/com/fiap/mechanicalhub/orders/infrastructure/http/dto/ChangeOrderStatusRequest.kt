package com.fiap.mechanicalhub.orders.infrastructure.http.dto

import com.fiap.mechanicalhub.orders.domain.enums.OrderStatusEnum
import jakarta.validation.constraints.NotNull

data class ChangeOrderStatusRequest(
    @field:NotNull val status: OrderStatusEnum?,
)
