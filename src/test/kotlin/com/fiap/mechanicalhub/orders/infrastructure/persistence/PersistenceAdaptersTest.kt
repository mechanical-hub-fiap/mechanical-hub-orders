package com.fiap.mechanicalhub.orders.infrastructure.persistence

import com.fiap.mechanicalhub.orders.application.events.OutgoingEvent
import com.fiap.mechanicalhub.orders.application.ports.ServiceOrderFilter
import com.fiap.mechanicalhub.orders.domain.enums.OrderStatusEnum
import com.fiap.mechanicalhub.orders.domain.enums.TaskStatusEnum
import com.fiap.mechanicalhub.orders.fixtures.FIXED_NOW
import com.fiap.mechanicalhub.orders.fixtures.Seeds
import com.fiap.mechanicalhub.orders.fixtures.aServiceOrder
import com.fiap.mechanicalhub.orders.infrastructure.messaging.OutgoingEventCodec
import com.fiap.mechanicalhub.orders.infrastructure.messaging.outbox.JdbcOutboxStore
import com.fiap.mechanicalhub.orders.infrastructure.persistence.adapters.CustomerRepositoryAdapter
import com.fiap.mechanicalhub.orders.infrastructure.persistence.adapters.InboxAdapter
import com.fiap.mechanicalhub.orders.infrastructure.persistence.adapters.JdbcConsumerTransaction
import com.fiap.mechanicalhub.orders.infrastructure.persistence.adapters.RepairRepositoryAdapter
import com.fiap.mechanicalhub.orders.infrastructure.persistence.adapters.ServiceOrderRepositoryAdapter
import com.fiap.mechanicalhub.orders.infrastructure.persistence.adapters.VehicleRepositoryAdapter
import com.fiap.mechanicalhub.orders.infrastructure.persistence.entities.CustomerEntity
import com.fiap.mechanicalhub.orders.infrastructure.persistence.entities.EmployeeEntity
import com.fiap.mechanicalhub.orders.infrastructure.persistence.entities.OutboxEventEntity
import com.fiap.mechanicalhub.orders.infrastructure.persistence.entities.OutboxStatus
import com.fiap.mechanicalhub.orders.infrastructure.persistence.entities.RepairEntity
import com.fiap.mechanicalhub.orders.infrastructure.persistence.entities.VehicleEntity
import com.fiap.mechanicalhub.orders.infrastructure.persistence.mappers.toDomain
import com.fiap.mechanicalhub.orders.infrastructure.persistence.mappers.toNewEntity
import com.fiap.mechanicalhub.orders.infrastructure.persistence.mappers.updateFrom
import com.fiap.mechanicalhub.orders.infrastructure.persistence.repositories.CustomerJpaRepository
import com.fiap.mechanicalhub.orders.infrastructure.persistence.repositories.RepairJpaRepository
import com.fiap.mechanicalhub.orders.infrastructure.persistence.repositories.ServiceOrderJpaRepository
import com.fiap.mechanicalhub.orders.infrastructure.persistence.repositories.VehicleJpaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.orm.ObjectOptimisticLockingFailureException
import tools.jackson.databind.json.JsonMapper
import java.math.BigDecimal
import java.sql.ResultSet
import java.time.Clock
import java.time.ZoneOffset
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PersistenceAdaptersTest {
    private val clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC)
    private val jpa = mockk<ServiceOrderJpaRepository>()
    private val entityManager = mockk<EntityManager>(relaxed = true)
    private val adapter = ServiceOrderRepositoryAdapter(jpa, entityManager, clock)

    @Test
    fun `given a new order when saving then it is persisted and flushed`() {
        val order = aServiceOrder()
        every { jpa.findById(order.id) } returns Optional.empty()

        val saved = adapter.save(order)

        assertEquals(order.id, saved.id)
        verify { entityManager.persist(any<Any>()) }
        verify { entityManager.flush() }
    }

    @Test
    fun `given an existing order when saving then the managed entity is updated`() {
        val order = aServiceOrder()
        val entity = order.toNewEntity(FIXED_NOW)
        every { jpa.findById(order.id) } returns Optional.of(entity)
        val changed = order.copy(status = OrderStatusEnum.PREPARING, budget = BigDecimal.ONE)

        val saved = adapter.save(changed)

        assertEquals(OrderStatusEnum.PREPARING, saved.status)
        assertEquals(BigDecimal.ONE, entity.budget)
    }

    @Test
    fun `given a stale version when saving then it fails with an optimistic lock error`() {
        val order = aServiceOrder()
        every { jpa.findById(order.id) } returns Optional.of(order.toNewEntity(FIXED_NOW).apply { version = 5 })

        assertFailsWith<ObjectOptimisticLockingFailureException> { adapter.save(order) }
    }

    @Test
    fun `given lookups when finding then the entity is mapped to the domain`() {
        val order = aServiceOrder()
        every { jpa.findById(order.id) } returns Optional.of(order.toNewEntity(FIXED_NOW))
        every { jpa.findById(neq(order.id)) } returns Optional.empty()
        every { jpa.findByOrderNumber("OS-1") } returns order.toNewEntity(FIXED_NOW)
        every { jpa.nextOrderSequence() } returns 7

        assertEquals(order.id, adapter.findById(order.id)?.id)
        assertNull(adapter.findById(UUID.randomUUID()))
        assertEquals(order.id, adapter.findByOrderNumber("OS-1")?.id)
        assertEquals(7, adapter.nextOrderSequence())
    }

    @Test
    fun `given a filter when searching then specification and sort are passed to the repository`() {
        val order = aServiceOrder()
        every {
            jpa.findAll(
                any<Specification<com.fiap.mechanicalhub.orders.infrastructure.persistence.entities.ServiceOrderEntity>>(),
                any<Sort>(),
            )
        } returns
            listOf(order.toNewEntity(FIXED_NOW))

        val result =
            adapter.search(ServiceOrderFilter(OrderStatusEnum.IN_DIAGNOSIS, Seeds.CUSTOMER_CARLOS, Seeds.MECHANIC))

        assertEquals(1, result.size)
    }

    @Test
    fun `given task changes when updating the entity then tasks are synced and new ones added`() {
        val order = aServiceOrder(taskStatuses = listOf(TaskStatusEnum.PENDING))
        val entity = order.toNewEntity(FIXED_NOW)
        val started = order.tasks.first().copy(status = TaskStatusEnum.STARTED, startedAt = FIXED_NOW)
        val added = aServiceOrder(taskStatuses = listOf(TaskStatusEnum.PENDING)).tasks.first()

        entity.updateFrom(order.copy(tasks = listOf(started, added), hasStockPending = true), FIXED_NOW)

        assertEquals(2, entity.tasks.size)
        assertEquals(TaskStatusEnum.STARTED, entity.tasks.first().status)
        assertTrue(entity.hasStockPending)
        assertEquals(2, entity.toDomain().tasks.size)
    }

    @Test
    fun `given catalog adapters when finding then entities are mapped`() {
        val customers = mockk<CustomerJpaRepository>()
        val vehicles = mockk<VehicleJpaRepository>()
        val repairs = mockk<RepairJpaRepository>()
        val customerId = UUID.randomUUID()
        val repairId = UUID.randomUUID()
        every { customers.findByIdAndDeletedAtIsNull(customerId) } returns
            CustomerEntity(customerId, "Ana", "123", "11", "a@a.com", null)
        every { customers.findByIdAndDeletedAtIsNull(neq(customerId)) } returns null
        every { vehicles.findByIdAndDeletedAtIsNull(any()) } returns VehicleEntity(Seeds.VEHICLE_GOL, customerId, "ABC", "VW", "Gol", null)
        every { repairs.findAllByActiveTrueAndDeletedAtIsNullOrderByName() } returns
            listOf(RepairEntity(repairId, "Freio", null, true, null))
        every { repairs.findAllByIdInAndDeletedAtIsNull(any()) } returns listOf(RepairEntity(repairId, "Freio", "d", true, null))

        assertEquals("Ana", CustomerRepositoryAdapter(customers).findById(customerId)?.name)
        assertNull(CustomerRepositoryAdapter(customers).findById(UUID.randomUUID()))
        assertEquals("Gol", VehicleRepositoryAdapter(vehicles).findById(Seeds.VEHICLE_GOL)?.model)
        val repairAdapter = RepairRepositoryAdapter(repairs)
        assertEquals("Freio", repairAdapter.findAllActive().single().name)
        assertEquals("Freio", repairAdapter.findAllByIds(listOf(repairId)).single().name)
        assertTrue(repairAdapter.findAllByIds(emptyList()).isEmpty())
    }

    @Test
    fun `given entities when reading and writing properties then values are kept`() {
        val employee = EmployeeEntity(UUID.randomUUID(), "Joe", "j@j.com", null)
        employee.name = "Joel"
        val outbox = OutboxEventEntity(UUID.randomUUID(), UUID.randomUUID(), "orderApproved", "{}", FIXED_NOW)
        outbox.status = OutboxStatus.SENT
        outbox.attempts = 2
        outbox.lastError = "x"
        outbox.sentAt = FIXED_NOW
        outbox.sequence = 3

        assertEquals("Joel", employee.name)
        assertEquals(OutboxStatus.SENT, outbox.status)
        assertEquals(3, outbox.sequence)
        assertEquals(2, outbox.attempts)
        assertNotNull(outbox.sentAt)
    }

    @Test
    fun `given the jdbc outbox store when operating then the expected sql is executed`() {
        val jdbc = mockk<JdbcTemplate>(relaxed = true)
        val store = JdbcOutboxStore(jdbc)
        every { jdbc.queryForObject(any<String>(), Boolean::class.java, any()) } returns true
        val rs = mockk<ResultSet>()
        val eventId = UUID.randomUUID()
        every { rs.getLong("sequence") } returns 9
        every { rs.getObject("event_id", UUID::class.java) } returns eventId
        every { rs.getObject("aggregate_id", UUID::class.java) } returns eventId
        every { rs.getString("event_type") } returns "orderApproved"
        every { rs.getString("payload") } returns "{}"
        every { jdbc.query(any<String>(), any<RowMapper<Any>>(), any<Any>()) } answers {
            listOf(secondArg<RowMapper<Any>>().mapRow(rs, 0))
        }

        assertTrue(store.tryAcquireLeadership())
        val pending = store.findPending(10)
        store.markSent(9, clock)
        store.registerFailure(9, "e".repeat(3000))

        assertEquals(9, pending.single().sequence)
        verify(exactly = 2) { jdbc.update(any<String>(), *anyVararg()) }
    }

    @Test
    fun `given the inbox adapter when registering and finding then codec and jdbc are used`() {
        val jdbc = mockk<JdbcTemplate>(relaxed = true)
        val codec = OutgoingEventCodec(JsonMapper.builder().build())
        val inbox = InboxAdapter(jdbc, codec, clock)
        val event = OutgoingEvent.OrderApproved(UUID.randomUUID(), UUID.randomUUID())
        val rs = mockk<ResultSet>()
        every { rs.getString("response") } returns codec.encode(event)
        every { jdbc.query(any<String>(), any<RowMapper<Any>>(), any<Any>()) } answers {
            listOf(secondArg<RowMapper<Any>>().mapRow(rs, 0))
        }

        inbox.register("key", event)
        inbox.register("key2", null)
        val found = inbox.find("key")

        assertEquals(event, found?.response)
        verify(exactly = 2) { jdbc.update(any<String>(), *anyVararg()) }
    }

    @Test
    fun `given the consumer transaction when applying the lock timeout then it runs set local`() {
        val jdbc = mockk<JdbcTemplate>(relaxed = true)

        JdbcConsumerTransaction(jdbc).applyLockTimeout()

        val sql = slot<String>()
        verify { jdbc.execute(capture(sql)) }
        assertFalse(sql.captured.isBlank())
    }
}
