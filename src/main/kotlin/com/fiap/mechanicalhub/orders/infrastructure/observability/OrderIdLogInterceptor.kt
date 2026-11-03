package com.fiap.mechanicalhub.orders.infrastructure.observability

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.HandlerMapping

/** Coloca o `{orderId}` da rota no MDC durante a requisicao. */
class OrderIdLogInterceptor : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        val variables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) as? Map<*, *>
        LogContext.put(LogContext.ORDER_ID, variables?.get(LogContext.ORDER_ID))
        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?,
    ) {
        MDC.remove(LogContext.ORDER_ID)
    }
}
