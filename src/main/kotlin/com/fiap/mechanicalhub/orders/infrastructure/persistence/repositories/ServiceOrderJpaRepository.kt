package com.fiap.mechanicalhub.orders.infrastructure.persistence.repositories

import com.fiap.mechanicalhub.orders.infrastructure.persistence.entities.ServiceOrderEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface ServiceOrderJpaRepository :
    JpaRepository<ServiceOrderEntity, UUID>,
    JpaSpecificationExecutor<ServiceOrderEntity> {
    fun findByOrderNumber(orderNumber: String): ServiceOrderEntity?

    @Query(value = "SELECT nextval('service_order_number_seq')", nativeQuery = true)
    fun nextOrderSequence(): Long
}
