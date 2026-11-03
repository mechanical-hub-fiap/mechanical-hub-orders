package com.fiap.mechanicalhub.orders.infrastructure.http

import com.fiap.mechanicalhub.orders.application.usecases.order.ServiceOrderDetails
import com.fiap.mechanicalhub.orders.domain.enums.OrderStatusEnum
import com.fiap.mechanicalhub.orders.domain.enums.TaskStatusEnum
import com.fiap.mechanicalhub.orders.domain.exceptions.ServiceOrderNotFoundException
import com.fiap.mechanicalhub.orders.fixtures.aRepair
import com.fiap.mechanicalhub.orders.fixtures.aServiceOrder
import com.fiap.mechanicalhub.orders.infrastructure.http.dto.toResponse
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.dao.QueryTimeoutException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RestStepTest {
    private val log = LoggerFactory.getLogger(RestStepTest::class.java)
    private val route = RouteStep("STATUS", "start", "end")

    @Test
    fun `given a block that succeeds when running then it returns the value`() {
        assertEquals(42, restStep(log, UUID.randomUUID(), route) { 42 })
    }

    @Test
    fun `given rule, conflict, transient and unexpected errors when running then each one is rethrown`() {
        listOf<Exception>(
            ServiceOrderNotFoundException("x"),
            IllegalArgumentException("bad"),
            ObjectOptimisticLockingFailureException(String::class.java, "id"),
            QueryTimeoutException("slow"),
            IllegalStateException("boom"),
        ).forEach { error ->
            val thrown = assertFailsWith<Exception> { restStep<Unit>(log, null, route) { throw error } }
            assertEquals(error, thrown)
        }
    }

    @Test
    fun `given statuses when mapping then name and display name are exposed`() {
        assertEquals("IN_DIAGNOSIS", OrderStatusEnum.IN_DIAGNOSIS.toResponse().code)
        assertEquals(TaskStatusEnum.PENDING.displayName, TaskStatusEnum.PENDING.toResponse().displayName)
    }

    @Test
    fun `given an order with repair names when mapping then tasks carry the names`() {
        val order = aServiceOrder(taskStatuses = listOf(TaskStatusEnum.PENDING, TaskStatusEnum.STARTED))
        val names = order.repairIds.associateWith { "Reparo" }

        val response = ServiceOrderDetails(order, names).toResponse()

        assertEquals(2, response.repairs.size)
        assertEquals("Reparo", response.repairs.first().name)
        assertEquals(order.orderNumber, response.orderNumber)
    }

    @Test
    fun `given a repair when mapping then it keeps id and name`() {
        val repair = aRepair(name = "Freio")

        val response = repair.toResponse()

        assertEquals(repair.id, response.id)
        assertEquals("Freio", response.name)
    }
}
