package com.fiap.mechanicalhub.orders.infrastructure.messaging.outbox

import com.fiap.mechanicalhub.orders.infrastructure.config.OrdersProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock

@Configuration
class OutboxRelayConfig {
    @Bean
    fun outboxRelay(
        store: OutboxStore,
        publisher: OutboxMessagePublisher,
        transactionTemplate: TransactionTemplate,
        clock: Clock,
        properties: OrdersProperties,
    ): OutboxRelay = OutboxRelay(store, publisher, transactionTemplate, clock, properties.outbox.batchSize)
}
