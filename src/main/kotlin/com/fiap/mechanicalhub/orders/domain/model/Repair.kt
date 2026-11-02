package com.fiap.mechanicalhub.orders.domain.model

import java.util.UUID

/** Reparo do catalogo (somente leitura, sem preco). */
data class Repair(
    val id: UUID,
    val name: String,
    val description: String?,
    val active: Boolean,
)
