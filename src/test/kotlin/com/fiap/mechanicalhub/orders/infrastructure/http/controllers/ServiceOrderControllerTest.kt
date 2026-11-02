package com.fiap.mechanicalhub.orders.infrastructure.http.controllers

import com.fiap.mechanicalhub.orders.application.ports.ServiceOrderFilter
import com.fiap.mechanicalhub.orders.application.usecases.order.AddRepairsUseCase
import com.fiap.mechanicalhub.orders.application.usecases.order.ChangeOrderStatusUseCase
import com.fiap.mechanicalhub.orders.application.usecases.order.OpenServiceOrderCommand
import com.fiap.mechanicalhub.orders.application.usecases.order.OpenServiceOrderUseCase
import com.fiap.mechanicalhub.orders.application.usecases.order.QueryServiceOrdersUseCase
import com.fiap.mechanicalhub.orders.application.usecases.order.ServiceOrderDetails
import com.fiap.mechanicalhub.orders.application.usecases.order.UpdateTaskStatusUseCase
import com.fiap.mechanicalhub.orders.domain.enums.OrderStatusEnum
import com.fiap.mechanicalhub.orders.domain.enums.TaskStatusEnum
import com.fiap.mechanicalhub.orders.fixtures.Seeds
import com.fiap.mechanicalhub.orders.fixtures.aServiceOrder
import com.fiap.mechanicalhub.orders.infrastructure.http.dto.AddRepairsRequest
import com.fiap.mechanicalhub.orders.infrastructure.http.dto.ChangeOrderStatusRequest
import com.fiap.mechanicalhub.orders.infrastructure.http.dto.OpenServiceOrderRequest
import com.fiap.mechanicalhub.orders.infrastructure.http.dto.RepairIdRequest
import com.fiap.mechanicalhub.orders.infrastructure.http.dto.UpdateTaskStatusRequest
import com.fiap.mechanicalhub.orders.infrastructure.security.GatewayPrincipal
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ServiceOrderControllerTest {
    private val open = mockk<OpenServiceOrderUseCase>()
    private val add = mockk<AddRepairsUseCase>(relaxed = true)
    private val change = mockk<ChangeOrderStatusUseCase>(relaxed = true)
    private val task = mockk<UpdateTaskStatusUseCase>(relaxed = true)
    private val query = mockk<QueryServiceOrdersUseCase>()
    private val controller = ServiceOrderController(open, add, change, task, query)

    private val order = aServiceOrder()
    private val details = ServiceOrderDetails(order, emptyMap())

    @Test
    fun `given a valid request when opening then it opens the order and returns it`() {
        val command = slot<OpenServiceOrderCommand>()
        every { open.execute(capture(command)) } returns order
        every { query.findById(order.id) } returns details
        val principal = GatewayPrincipal(Seeds.MECHANIC, "Mec", "MECHANICAL")

        val response =
            controller.open(
                OpenServiceOrderRequest(Seeds.CUSTOMER_CARLOS, Seeds.VEHICLE_GOL, listOf(Seeds.OIL_CHANGE), "Barulho"),
                principal,
            )

        assertEquals(order.id, response.id)
        assertEquals(Seeds.MECHANIC, command.captured.userId)
        assertEquals(listOf(Seeds.OIL_CHANGE), command.captured.repairIds)
    }

    @Test
    fun `given repairs when adding then it delegates and returns the order`() {
        every { query.findById(order.id) } returns details

        val response = controller.addRepairs(order.id, AddRepairsRequest(listOf(RepairIdRequest(Seeds.BRAKES), RepairIdRequest(null))))

        assertEquals(order.id, response.id)
        verify { add.execute(order.id, listOf(Seeds.BRAKES)) }
    }

    @Test
    fun `given each status when changing then it delegates with that status`() {
        every { query.findById(order.id) } returns details

        listOf(OrderStatusEnum.PREPARING, OrderStatusEnum.IN_PROGRESS, OrderStatusEnum.FINISHED, OrderStatusEnum.DELIVERED).forEach {
            controller.changeStatus(order.id, ChangeOrderStatusRequest(it))
            verify { change.execute(order.id, it) }
        }
    }

    @Test
    fun `given a status without a dedicated route when changing then the generic route is used`() {
        every { query.findById(order.id) } returns details

        controller.changeStatus(order.id, ChangeOrderStatusRequest(OrderStatusEnum.CANCELED))

        verify { change.execute(order.id, OrderStatusEnum.CANCELED) }
    }

    @Test
    fun `given a task update when posting then it delegates`() {
        every { query.findById(order.id) } returns details

        controller.updateTaskStatus(UpdateTaskStatusRequest(order.id, Seeds.OIL_CHANGE, TaskStatusEnum.STARTED))

        verify { task.execute(order.id, Seeds.OIL_CHANGE, TaskStatusEnum.STARTED) }
    }

    @Test
    fun `given an order when finding by id then it returns the response`() {
        every { query.findById(order.id) } returns details

        assertEquals(order.orderNumber, controller.findById(order.id).orderNumber)
    }

    @Test
    fun `given filters when searching then they are forwarded`() {
        val filter = slot<ServiceOrderFilter>()
        every { query.search(capture(filter)) } returns listOf(details)

        val result = controller.search(OrderStatusEnum.IN_DIAGNOSIS, Seeds.CUSTOMER_CARLOS, null)

        assertEquals(1, result.size)
        assertEquals(OrderStatusEnum.IN_DIAGNOSIS, filter.captured.status)
        assertEquals(Seeds.CUSTOMER_CARLOS, filter.captured.customerId)
    }

    @Test
    fun `given a use case failure when opening then the exception propagates`() {
        every { open.execute(any()) } throws IllegalStateException("boom")

        assertFailsWith<IllegalStateException> {
            controller.open(
                OpenServiceOrderRequest(UUID.randomUUID(), UUID.randomUUID(), listOf(UUID.randomUUID()), "x"),
                GatewayPrincipal(Seeds.MECHANIC, null, "MECHANICAL"),
            )
        }
    }
}
