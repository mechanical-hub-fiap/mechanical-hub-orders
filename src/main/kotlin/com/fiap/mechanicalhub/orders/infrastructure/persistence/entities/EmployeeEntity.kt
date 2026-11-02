package com.fiap.mechanicalhub.orders.infrastructure.persistence.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/** Funcionario (`users`), lido para notificar o mecanico responsavel. */
@Entity
@Table(name = "users")
class EmployeeEntity(
    @Id
    var id: UUID,
    @Column(nullable = false)
    var name: String,
    @Column(nullable = false)
    var email: String,
    @Column(name = "deleted_at")
    var deletedAt: Instant?,
)
