package com.fiap.mechanicalhub.orders.infrastructure.persistence.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/** Somente leitura: clientes vem das seeds (sem endpoint de cadastro nesta fase). */
@Entity
@Table(name = "customers")
class CustomerEntity(
    @Id
    var id: UUID,
    @Column(nullable = false)
    var name: String,
    @Column(name = "document_number", nullable = false)
    var documentNumber: String,
    @Column(nullable = false)
    var telephone: String,
    @Column(nullable = false)
    var email: String,
    @Column(name = "deleted_at")
    var deletedAt: Instant?,
)
