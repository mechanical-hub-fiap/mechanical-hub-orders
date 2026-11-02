package com.fiap.mechanicalhub.orders.application.ports

/** Ajustes da transacao do consumidor. */
fun interface ConsumerTransaction {
    /** `SET LOCAL lock_timeout`: espera por lock vira erro transitorio retentavel. */
    fun applyLockTimeout()
}
