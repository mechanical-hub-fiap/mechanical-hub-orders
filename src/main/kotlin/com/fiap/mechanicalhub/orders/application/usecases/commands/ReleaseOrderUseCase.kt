package com.fiap.mechanicalhub.orders.application.usecases.commands

import com.fiap.mechanicalhub.orders.application.commands.OrderCommand
import com.fiap.mechanicalhub.orders.application.notifications.Notification
import com.fiap.mechanicalhub.orders.application.ports.ServiceOrderRepository
import com.fiap.mechanicalhub.orders.application.usecases.order.requireById
import com.fiap.mechanicalhub.orders.logging.FlowLog
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** `releaseOrder` → `READY_FOR_EXECUTION` e aviso ao mecanico (TD_finishedOS.md 18). */
@Service
class ReleaseOrderUseCase(
    private val serviceOrderRepository: ServiceOrderRepository,
    private val runner: IdempotentCommandRunner,
) {
    @Transactional
    fun execute(command: OrderCommand.ReleaseOrder): CommandOutcome =
        runner.run(command.idempotencyKey) {
            val order = serviceOrderRepository.save(serviceOrderRepository.requireById(command.orderId).releaseForExecution())
            FlowLog.detail("orderStatus" to order.status)
            CommandEffect(notification = Notification.OrderReleased(order.id, order.orderNumber, order.responsibleUserId))
        }
}
