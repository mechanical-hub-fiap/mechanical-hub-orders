package com.fiap.mechanicalhub.orders.infrastructure.persistence.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "vehicles")
class VehicleEntity(
    @Id
    var id: UUID,
    @Column(name = "customer_id", nullable = false)
    var customerId: UUID,
    @Column(name = "license_plate", nullable = false)
    var licensePlate: String,
    @Column(nullable = false)
    var brand: String,
    @Column(nullable = false)
    var model: String,
    @Column(name = "deleted_at")
    var deletedAt: Instant?,
)
