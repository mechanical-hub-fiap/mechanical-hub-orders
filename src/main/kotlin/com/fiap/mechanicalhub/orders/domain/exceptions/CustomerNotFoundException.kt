package com.fiap.mechanicalhub.orders.domain.exceptions

import java.util.UUID

class CustomerNotFoundException(
    customerId: UUID,
) : NotFoundException("Cliente $customerId nao encontrado")
