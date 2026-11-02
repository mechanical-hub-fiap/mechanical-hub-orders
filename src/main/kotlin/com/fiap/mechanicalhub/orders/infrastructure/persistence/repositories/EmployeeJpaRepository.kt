package com.fiap.mechanicalhub.orders.infrastructure.persistence.repositories

import com.fiap.mechanicalhub.orders.infrastructure.persistence.entities.EmployeeEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface EmployeeJpaRepository : JpaRepository<EmployeeEntity, UUID> {
    fun findByIdAndDeletedAtIsNull(id: UUID): EmployeeEntity?
}
