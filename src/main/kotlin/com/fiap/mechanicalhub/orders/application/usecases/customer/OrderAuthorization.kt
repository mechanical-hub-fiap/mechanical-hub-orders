package com.fiap.mechanicalhub.orders.application.usecases.customer

import com.fiap.mechanicalhub.orders.domain.approval.matchesApprovalToken
import com.fiap.mechanicalhub.orders.domain.exceptions.ServiceOrderNotFoundException
import com.fiap.mechanicalhub.orders.domain.model.ServiceOrder

/**
 * Token ausente, invalido ou ordem sem token (ainda sem orcamento) → 404, sem
 * revelar se a ordem existe (TD_finishedOS.md passo 12).
 */
internal fun ServiceOrder?.authorizedBy(
    approvalToken: String?,
    reference: String,
): ServiceOrder {
    val tokenHash = this?.approvalTokenHash
    if (this == null || tokenHash == null || !tokenHash.matchesApprovalToken(approvalToken)) {
        throw ServiceOrderNotFoundException(reference)
    }
    return this
}
