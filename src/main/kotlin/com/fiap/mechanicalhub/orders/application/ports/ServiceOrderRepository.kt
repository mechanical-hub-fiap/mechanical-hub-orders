package com.fiap.mechanicalhub.orders.application.ports

import com.fiap.mechanicalhub.orders.domain.model.ServiceOrder
import java.util.UUID

interface ServiceOrderRepository {
    fun findById(id: UUID): ServiceOrder?

    fun findByOrderNumber(orderNumber: String): ServiceOrder?

    fun search(filter: ServiceOrderFilter): List<ServiceOrder>

    /**
     * Grava a ordem com controle otimista: se a `version` lida ja mudou no banco,
     * lanca o conflito do Spring (`OptimisticLockingFailureException`).
     */
    fun save(order: ServiceOrder): ServiceOrder

    /** Proximo valor da sequencia usada no numero da ordem. */
    fun nextOrderSequence(): Long
}
