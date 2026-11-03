package com.fiap.mechanicalhub.orders.infrastructure.persistence.repositories

import com.fiap.mechanicalhub.orders.infrastructure.persistence.entities.VehicleEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface VehicleJpaRepository : JpaRepository<VehicleEntity, UUID> {
    fun findByIdAndDeletedAtIsNull(id: UUID): VehicleEntity?
}
