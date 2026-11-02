package com.fiap.mechanicalhub.orders.infrastructure.persistence.adapters

import com.fiap.mechanicalhub.orders.application.ports.CustomerRepository
import com.fiap.mechanicalhub.orders.domain.model.Customer
import com.fiap.mechanicalhub.orders.infrastructure.persistence.mappers.toDomain
import com.fiap.mechanicalhub.orders.infrastructure.persistence.repositories.CustomerJpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class CustomerRepositoryAdapter(
    private val jpaRepository: CustomerJpaRepository,
) : CustomerRepository {
    override fun findById(id: UUID): Customer? = jpaRepository.findByIdAndDeletedAtIsNull(id)?.toDomain()
}
