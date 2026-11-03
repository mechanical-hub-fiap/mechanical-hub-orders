package com.fiap.mechanicalhub.orders.infrastructure.persistence.adapters

import com.fiap.mechanicalhub.orders.application.ports.RepairRepository
import com.fiap.mechanicalhub.orders.domain.model.Repair
import com.fiap.mechanicalhub.orders.infrastructure.persistence.mappers.toDomain
import com.fiap.mechanicalhub.orders.infrastructure.persistence.repositories.RepairJpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class RepairRepositoryAdapter(
    private val jpaRepository: RepairJpaRepository,
) : RepairRepository {
    override fun findAllActive(): List<Repair> = jpaRepository.findAllByActiveTrueAndDeletedAtIsNullOrderByName().map { it.toDomain() }

    override fun findAllByIds(ids: Collection<UUID>): List<Repair> =
        if (ids.isEmpty()) emptyList() else jpaRepository.findAllByIdInAndDeletedAtIsNull(ids).map { it.toDomain() }
}
