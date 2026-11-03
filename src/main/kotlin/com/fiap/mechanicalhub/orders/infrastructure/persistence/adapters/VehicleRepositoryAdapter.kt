package com.fiap.mechanicalhub.orders.infrastructure.persistence.adapters

import com.fiap.mechanicalhub.orders.application.ports.VehicleRepository
import com.fiap.mechanicalhub.orders.domain.model.Vehicle
import com.fiap.mechanicalhub.orders.infrastructure.persistence.mappers.toDomain
import com.fiap.mechanicalhub.orders.infrastructure.persistence.repositories.VehicleJpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class VehicleRepositoryAdapter(
    private val jpaRepository: VehicleJpaRepository,
) : VehicleRepository {
    override fun findById(id: UUID): Vehicle? = jpaRepository.findByIdAndDeletedAtIsNull(id)?.toDomain()
}
