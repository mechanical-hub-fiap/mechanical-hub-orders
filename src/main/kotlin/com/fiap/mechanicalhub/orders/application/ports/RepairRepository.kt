package com.fiap.mechanicalhub.orders.application.ports

import com.fiap.mechanicalhub.orders.domain.model.Repair
import java.util.UUID

interface RepairRepository {
    fun findAllActive(): List<Repair>

    fun findAllByIds(ids: Collection<UUID>): List<Repair>
}
