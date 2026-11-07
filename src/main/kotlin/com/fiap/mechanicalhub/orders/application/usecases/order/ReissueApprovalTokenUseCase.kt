package com.fiap.mechanicalhub.orders.application.usecases.order

import com.fiap.mechanicalhub.orders.application.ports.ServiceOrderRepository
import com.fiap.mechanicalhub.orders.domain.approval.ApprovalToken
import com.fiap.mechanicalhub.orders.logging.FlowLog
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.util.UUID

/** POST /orders/{orderId}/approval-token: `reissueApprovalToken()`. Nao publica evento nem notifica o cliente. */
@Service
class ReissueApprovalTokenUseCase(
    private val serviceOrderRepository: ServiceOrderRepository,
    private val secureRandom: SecureRandom,
) {
    @Transactional
    fun execute(
        orderId: UUID,
        requestedBy: UUID,
    ): ReissuedApprovalToken {
        val token = ApprovalToken.generate(secureRandom)
        val order = serviceOrderRepository.save(serviceOrderRepository.requireById(orderId).reissueApprovalToken(token.hash()))
        FlowLog.detail("orderStatus" to order.status, "requestedBy" to requestedBy)
        return ReissuedApprovalToken(order.id, order.orderNumber, token)
    }
}
