package com.fiap.mechanicalhub.orders.application.usecases.order

import com.fiap.mechanicalhub.orders.domain.model.ServiceOrder
import java.util.UUID

/** Ordem acompanhada do nome de cada reparo do catalogo. */
data class ServiceOrderDetails(
    val order: ServiceOrder,
    val repairNames: Map<UUID, String>,
)
