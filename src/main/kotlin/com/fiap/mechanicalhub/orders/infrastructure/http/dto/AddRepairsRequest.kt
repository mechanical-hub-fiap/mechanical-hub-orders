package com.fiap.mechanicalhub.orders.infrastructure.http.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty

data class AddRepairsRequest(
    @field:NotEmpty @field:Valid val repairs: List<RepairIdRequest>?,
)
