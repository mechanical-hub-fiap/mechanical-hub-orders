package com.fiap.mechanicalhub.orders.domain.exceptions

import com.fiap.mechanicalhub.orders.domain.enums.OrderStatusEnum
import java.util.UUID

/** Transicao de status fora da tabela de order_status.md (HTTP 409, comando: erro permanente). */
class InvalidOrderTransitionException(
    orderId: UUID,
    currentStatus: OrderStatusEnum,
    action: String,
) : DomainException("Ordem $orderId em $currentStatus nao permite $action")
