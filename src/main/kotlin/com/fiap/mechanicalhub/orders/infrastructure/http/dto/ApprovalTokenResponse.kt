package com.fiap.mechanicalhub.orders.infrastructure.http.dto

import java.util.UUID

/** Token em texto puro: so aparece nesta resposta. Nao entra em log. */
data class ApprovalTokenResponse(
    val orderId: UUID,
    val orderNumber: String,
    val approvalToken: String,
    val approvalLink: String,
)
