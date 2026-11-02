package com.fiap.mechanicalhub.orders.infrastructure.persistence.repositories

import com.fiap.mechanicalhub.orders.infrastructure.persistence.entities.OutboxEventEntity
import org.springframework.data.jpa.repository.JpaRepository

interface OutboxEventJpaRepository : JpaRepository<OutboxEventEntity, Long>
