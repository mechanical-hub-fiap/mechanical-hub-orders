package com.fiap.mechanicalhub.orders.domain.exceptions

/** Regra do agregado violada com a ordem no status correto (HTTP 409). */
class OrderRuleViolationException(
    message: String,
) : DomainException(message)
