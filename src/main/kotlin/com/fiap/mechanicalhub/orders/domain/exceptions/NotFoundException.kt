package com.fiap.mechanicalhub.orders.domain.exceptions

/** Recurso inexistente (HTTP 404). */
sealed class NotFoundException(
    message: String,
) : DomainException(message)
