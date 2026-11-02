package com.fiap.mechanicalhub.orders.infrastructure.persistence.adapters

import com.fiap.mechanicalhub.orders.application.ports.ConsumerTransaction
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class JdbcConsumerTransaction(
    private val jdbcTemplate: JdbcTemplate,
) : ConsumerTransaction {
    override fun applyLockTimeout() {
        jdbcTemplate.execute("SET LOCAL lock_timeout = '5s'")
    }
}
