package com.fiap.mechanicalhub.orders.infrastructure.persistence.entities

import com.fiap.mechanicalhub.orders.domain.enums.TaskStatusEnum
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "order_tasks")
class OrderTaskEntity(
    @Id
    var id: UUID,
    @Column(name = "service_id", nullable = false, updatable = false)
    var repairId: UUID,
    @Enumerated(EnumType.STRING)
    @Column(name = "service_status", nullable = false)
    var status: TaskStatusEnum,
    @Column(name = "started_at")
    var startedAt: Instant?,
    @Column(name = "finished_at")
    var finishedAt: Instant?,
)
