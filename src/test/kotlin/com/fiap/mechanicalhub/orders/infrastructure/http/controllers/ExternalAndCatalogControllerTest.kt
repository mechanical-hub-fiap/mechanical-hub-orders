package com.fiap.mechanicalhub.orders.infrastructure.http.controllers

import com.fiap.mechanicalhub.orders.application.usecases.catalog.ListRepairsUseCase
import com.fiap.mechanicalhub.orders.application.usecases.customer.ApproveOrderUseCase
import com.fiap.mechanicalhub.orders.application.usecases.customer.CustomerOrderView
import com.fiap.mechanicalhub.orders.application.usecases.customer.GetCustomerOrderUseCase
import com.fiap.mechanicalhub.orders.application.usecases.customer.RejectOrderUseCase
import com.fiap.mechanicalhub.orders.domain.enums.OrderStatusEnum
import com.fiap.mechanicalhub.orders.fixtures.FIXED_NOW
import com.fiap.mechanicalhub.orders.fixtures.aRepair
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.assertEquals

class ExternalAndCatalogControllerTest {
    private val get = mockk<GetCustomerOrderUseCase>()
    private val approve = mockk<ApproveOrderUseCase>(relaxed = true)
    private val reject = mockk<RejectOrderUseCase>(relaxed = true)
    private val external = ExternalController(get, approve, reject)

    @Test
    fun `given an order number and token when finding then it maps the customer view`() {
        val view =
            CustomerOrderView(
                orderId = UUID.randomUUID(),
                orderNumber = "OS-1",
                customerName = "Carlos",
                vehicleLicensePlate = "BRA2E19",
                vehicleBrand = "VW",
                vehicleModel = "Gol",
                status = OrderStatusEnum.WAITING_APPROVAL,
                budget = BigDecimal.TEN,
                services = listOf("Troca de oleo"),
                openedAt = FIXED_NOW,
                completedAt = null,
                deliveredAt = null,
            )
        every { get.execute("OS-1", "tok") } returns view

        val response = external.find("OS-1", "tok")

        assertEquals("OS-1", response.orderNumber)
        assertEquals("WAITING_APPROVAL", response.status.code)
        assertEquals("Gol", response.vehicleModel)
    }

    @Test
    fun `given a token when approving and rejecting then the use cases are called`() {
        val id = UUID.randomUUID()

        external.approve(id, "tok")
        external.reject(id, null)

        verify { approve.execute(id, "tok") }
        verify { reject.execute(id, null) }
    }

    @Test
    fun `given active repairs when listing then they are mapped`() {
        val list = mockk<ListRepairsUseCase>()
        every { list.execute() } returns listOf(aRepair(name = "Freio"))

        val result = CatalogController(list).list()

        assertEquals("Freio", result.single().name)
    }
}
