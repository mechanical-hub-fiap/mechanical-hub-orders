package com.fiap.mechanicalhub.orders.infrastructure.observability

import com.fiap.mechanicalhub.orders.infrastructure.notification.channels.EmailSender
import com.fiap.mechanicalhub.orders.infrastructure.notification.channels.NotificationMessage
import com.fiap.mechanicalhub.orders.infrastructure.notification.channels.Recipient
import com.fiap.mechanicalhub.orders.infrastructure.notification.channels.WhatsAppMessenger
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.servlet.HandlerMapping
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WebAndChannelsTest {
    private val interceptor = OrderIdLogInterceptor()
    private val response = MockHttpServletResponse()

    @Test
    fun `given a route with orderId when handling then the MDC is set and cleared afterwards`() {
        val request = MockHttpServletRequest()
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, mapOf(LogContext.ORDER_ID to "abc"))

        assertTrue(interceptor.preHandle(request, response, Any()))
        assertEquals("abc", MDC.get(LogContext.ORDER_ID))

        interceptor.afterCompletion(request, response, Any(), null)
        assertNull(MDC.get(LogContext.ORDER_ID))
    }

    @Test
    fun `given a route without variables when handling then no orderId is set`() {
        MDC.remove(LogContext.ORDER_ID)

        interceptor.preHandle(MockHttpServletRequest(), response, Any())

        assertNull(MDC.get(LogContext.ORDER_ID))
    }

    @Test
    fun `given the web config when adding interceptors then the order id interceptor is registered`() {
        val registry = mockk<InterceptorRegistry>(relaxed = true)

        LogContextWebConfig().addInterceptors(registry)

        verify { registry.addInterceptor(any<OrderIdLogInterceptor>()) }
    }

    @Test
    fun `given recipients with and without contacts when sending then channels never throw`() {
        val message = NotificationMessage("assunto", "corpo")

        EmailSender().send(Recipient("Ana", "a@a.com", "11"), message)
        EmailSender().send(Recipient("Ana", null, "11"), message)
        WhatsAppMessenger().send(Recipient("Ana", "a@a.com", "11"), message)
        WhatsAppMessenger().send(Recipient("Ana", "a@a.com", null), message)
    }
}
