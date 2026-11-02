package com.fiap.mechanicalhub.orders.infrastructure.persistence.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/** Catalogo de reparos (`services`); id = repairId. */
@Entity
@Table(name = "services")
class RepairEntity(
    @Id
    var id: UUID,
    @Column(nullable = false)
    var name: String,
    @Column
    var description: String?,
    @Column(nullable = false)
    var active: Boolean,
    @Column(name = "deleted_at")
    var deletedAt: Instant?,
)
