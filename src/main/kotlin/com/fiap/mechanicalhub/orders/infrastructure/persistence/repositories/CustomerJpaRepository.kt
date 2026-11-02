package com.fiap.mechanicalhub.orders.infrastructure.persistence.repositories

import com.fiap.mechanicalhub.orders.infrastructure.persistence.entities.CustomerEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CustomerJpaRepository : JpaRepository<CustomerEntity, UUID> {
    fun findByIdAndDeletedAtIsNull(id: UUID): CustomerEntity?
}
