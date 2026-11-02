package com.fiap.mechanicalhub.orders.infrastructure.persistence.repositories

import com.fiap.mechanicalhub.orders.infrastructure.persistence.entities.RepairEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RepairJpaRepository : JpaRepository<RepairEntity, UUID> {
    fun findAllByActiveTrueAndDeletedAtIsNullOrderByName(): List<RepairEntity>

    fun findAllByIdInAndDeletedAtIsNull(ids: Collection<UUID>): List<RepairEntity>
}
