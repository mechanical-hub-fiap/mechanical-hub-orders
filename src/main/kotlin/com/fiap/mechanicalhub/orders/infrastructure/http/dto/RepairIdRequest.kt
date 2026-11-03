package com.fiap.mechanicalhub.orders.infrastructure.http.dto

import jakarta.validation.constraints.NotNull
import java.util.UUID

data class RepairIdRequest(
    @field:NotNull val repairId: UUID?,
)
