package com.fiap.mechanicalhub.orders.application.ports

import com.fiap.mechanicalhub.orders.domain.model.Vehicle
import java.util.UUID

interface VehicleRepository {
    fun findById(id: UUID): Vehicle?
}
