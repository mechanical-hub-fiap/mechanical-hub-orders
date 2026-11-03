package com.fiap.mechanicalhub.orders.application.usecases.customer

import com.fiap.mechanicalhub.orders.application.events.OutgoingEvent
import com.fiap.mechanicalhub.orders.application.ports.OutboxPort
import com.fiap.mechanicalhub.orders.application.ports.ServiceOrderRepository
import com.fiap.mechanicalhub.orders.application.usecases.order.requireSaga
import com.fiap.mechanicalhub.orders.logging.FlowLog
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/** POST /mechanical-hub/service-orders/{orderId}/reject: `reject()` + `orderRejected`. */
@Service
class RejectOrderUseCase(
    private val serviceOrderRepository: ServiceOrderRepository,
    private val outbox: OutboxPort,
) {
    @Transactional
    fun execute(
        orderId: UUID,
        approvalToken: String?,
    ) {
        val order = serviceOrderRepository.findById(orderId).authorizedBy(approvalToken, orderId.toString())
        val rejected = serviceOrderRepository.save(order.reject())
        outbox.append(OutgoingEvent.OrderRejected(rejected.requireSaga(), rejected.id))
        FlowLog.detail("orderStatus" to rejected.status, "transactionId" to rejected.sagaTransactionId)
    }
}
