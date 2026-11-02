package com.fiap.mechanicalhub.orders.domain.exceptions

import java.util.UUID

class RepairNotFoundException(
    repairIds: Collection<UUID>,
) : NotFoundException("Reparo(s) inexistente(s) ou inativo(s): ${repairIds.joinToString()}")
