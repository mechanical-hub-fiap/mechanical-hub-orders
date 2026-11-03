package com.fiap.mechanicalhub.orders.infrastructure.http.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.UUID

/** Mesmo formato do `/open` do monolito. */
data class OpenServiceOrderRequest(
    @field:NotNull val customerId: UUID?,
    @field:NotNull val vehicleId: UUID?,
    @field:NotEmpty val serviceIds: List<UUID>?,
    @field:NotBlank @field:Size(max = 255) val requestDescription: String?,
)
