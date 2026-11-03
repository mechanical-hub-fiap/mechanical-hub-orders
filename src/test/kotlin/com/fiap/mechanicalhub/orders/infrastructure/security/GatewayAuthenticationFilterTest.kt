package com.fiap.mechanicalhub.orders.infrastructure.security

import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GatewayAuthenticationFilterTest {
    private val filter = GatewayAuthenticationFilter()
    private var chained = false
    private val chain = FilterChain { _, _ -> chained = true }

    @AfterEach
    fun clear() = SecurityContextHolder.clearContext()

    private fun run(vararg headers: Pair<String, String>) {
        SecurityContextHolder.clearContext()
        val request = MockHttpServletRequest().apply { headers.forEach { (k, v) -> addHeader(k, v) } }
        filter.doFilter(request, MockHttpServletResponse(), chain)
    }

    @Test
    fun `given valid gateway headers when filtering then the principal is authenticated`() {
        val id = UUID.randomUUID()

        run("x-user-id" to id.toString(), "x-user-role" to "mechanical", "x-user-name" to "Mec")

        val auth = assertNotNull(SecurityContextHolder.getContext().authentication)
        assertEquals(GatewayPrincipal(id, "Mec", "mechanical"), auth.principal)
        assertEquals("ROLE_MECHANICAL", auth.authorities.single().authority)
        assertEquals(id.toString(), auth.principal.toString())
        assertTrue(chained)
    }

    @Test
    fun `given no headers when filtering then the request stays anonymous`() {
        run()

        assertNull(SecurityContextHolder.getContext().authentication)
        assertTrue(chained)
    }

    @Test
    fun `given an invalid user id when filtering then the request stays anonymous`() {
        run("x-user-id" to "not-a-uuid", "x-user-role" to "ADMINISTRATOR")

        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `given a blank role when filtering then the request stays anonymous`() {
        run("x-user-id" to UUID.randomUUID().toString(), "x-user-role" to " ")

        assertNull(SecurityContextHolder.getContext().authentication)
    }
}
