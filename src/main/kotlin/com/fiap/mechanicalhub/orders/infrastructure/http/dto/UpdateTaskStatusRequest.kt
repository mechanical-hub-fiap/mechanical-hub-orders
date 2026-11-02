package com.fiap.mechanicalhub.orders.infrastructure.http.dto

import com.fiap.mechanicalhub.orders.domain.enums.TaskStatusEnum
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class UpdateTaskStatusRequest(
    @field:NotNull val orderId: UUID?,
    @field:NotNull val repairId: UUID?,
    @field:NotNull val status: TaskStatusEnum?,
)
