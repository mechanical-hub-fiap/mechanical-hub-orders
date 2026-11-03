package com.fiap.mechanicalhub.orders.infrastructure.http

/** Etapa de uma rota: `status` do log (logging-standard §3) e `msg` do `START` e do `END`. */
data class RouteStep(
    val status: String,
    val start: String,
    val end: String,
)
