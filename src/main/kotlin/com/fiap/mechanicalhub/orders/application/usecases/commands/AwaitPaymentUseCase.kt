package com.fiap.mechanicalhub.orders.application.usecases.commands

import com.fiap.mechanicalhub.orders.application.commands.OrderCommand
import com.fiap.mechanicalhub.orders.application.notifications.Notification
import com.fiap.mechanicalhub.orders.application.ports.ServiceOrderRepository
import com.fiap.mechanicalhub.orders.application.usecases.order.requireById
import com.fiap.mechanicalhub.orders.logging.FlowLog
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** `sendPaymentLink` → `AWAITING_PAYMENT` e link ao cliente (td_finished_os.md 15). */
@Service
class AwaitPaymentUseCase(
    private val serviceOrderRepository: ServiceOrderRepository,
    private val runner: IdempotentCommandRunner,
) {
    @Transactional
    fun execute(command: OrderCommand.SendPaymentLink): CommandOutcome =
        runner.run(command.idempotencyKey) {
            val order = serviceOrderRepository.save(serviceOrderRepository.requireById(command.orderId).awaitPayment())
            FlowLog.detail("orderStatus" to order.status)
            CommandEffect(
                notification = Notification.PaymentLinkReady(order.id, order.orderNumber, order.customerId, command.paymentLink),
            )
        }
}
