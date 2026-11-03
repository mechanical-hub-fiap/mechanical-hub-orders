package com.fiap.mechanicalhub.orders.domain.exceptions

/** Requisicao com referencias inconsistentes, como veiculo de outro cliente (HTTP 422). */
class InvalidOrderRequestException(
    message: String,
) : DomainException(message)
