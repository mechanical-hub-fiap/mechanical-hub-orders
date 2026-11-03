package com.fiap.mechanicalhub.orders.domain.exceptions

/** Raiz das excecoes de dominio; a traducao para HTTP/mensagem fica na infrastructure. */
sealed class DomainException(
    message: String,
) : RuntimeException(message)
