package com.fiap.mechanicalhub.orders.domain.exceptions

class ServiceOrderNotFoundException(
    reference: String,
) : NotFoundException("Ordem de servico $reference nao encontrada")
