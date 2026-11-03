package com.fiap.mechanicalhub.orders.application.usecases.customer

import com.fiap.mechanicalhub.orders.application.events.OutgoingEvent
import com.fiap.mechanicalhub.orders.application.ports.OutboxPort
import com.fiap.mechanicalhub.orders.application.ports.ServiceOrderRepository
import com.fiap.mechanicalhub.orders.application.usecases.order.requireSaga
import com.fiap.mechanicalhub.orders.logging.FlowLog
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/** POST /mechanical-hub/service-orders/{orderId}/approve: `approve()` + `orderApproved`. */
@Service
class ApproveOrderUseCase(
    private val serviceOrderRepository: ServiceOrderRepository,
    private val outbox: OutboxPort,
) {
    @Transactional
    fun execute(
        orderId: UUID,
        approvalToken: String?,
    ) {
        val order = serviceOrderRepository.findById(orderId).authorizedBy(approvalToken, orderId.toString())
        val approved = serviceOrderRepository.save(order.approve())
        outbox.append(OutgoingEvent.OrderApproved(approved.requireSaga(), approved.id))
        FlowLog.detail("orderStatus" to approved.status, "transactionId" to approved.sagaTransactionId)
    }
}
