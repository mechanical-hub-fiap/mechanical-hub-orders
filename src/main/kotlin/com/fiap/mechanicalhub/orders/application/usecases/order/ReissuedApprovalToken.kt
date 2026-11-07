package com.fiap.mechanicalhub.orders.application.usecases.order

import com.fiap.mechanicalhub.orders.domain.approval.ApprovalToken
import java.util.UUID

/** Token novo para uma ordem em `WAITING_APPROVAL`; o texto puro so existe na resposta, o banco guarda o SHA-256. */
data class ReissuedApprovalToken(
    val orderId: UUID,
    val orderNumber: String,
    val token: ApprovalToken,
)
