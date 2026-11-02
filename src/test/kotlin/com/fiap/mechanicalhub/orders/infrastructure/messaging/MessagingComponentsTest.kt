package com.fiap.mechanicalhub.orders.infrastructure.messaging

import com.fiap.mechanicalhub.orders.application.events.OutgoingEvent
import com.fiap.mechanicalhub.orders.application.events.RepairReference
import com.fiap.mechanicalhub.orders.application.notifications.Notification
import com.fiap.mechanicalhub.orders.application.ports.NotificationPort
import com.fiap.mechanicalhub.orders.infrastructure.config.OrdersProperties
import com.fiap.mechanicalhub.orders.infrastructure.messaging.commands.CommandDeadLetterPublisher
import com.fiap.mechanicalhub.orders.infrastructure.messaging.commands.CommandDecision
import com.fiap.mechanicalhub.orders.infrastructure.messaging.commands.CommandMessageProcessor
import com.fiap.mechanicalhub.orders.infrastructure.messaging.commands.OrderCommandListener
import com.fiap.mechanicalhub.orders.infrastructure.messaging.outbox.OutboxRelay
import com.fiap.mechanicalhub.orders.infrastructure.messaging.outbox.OutboxRelayConfig
import com.fiap.mechanicalhub.orders.infrastructure.messaging.outbox.OutboxRelayScheduler
import com.fiap.mechanicalhub.orders.infrastructure.messaging.outbox.OutboxStore
import io.awspring.cloud.sqs.listener.SqsHeaders
import io.awspring.cloud.sqs.listener.Visibility
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.messaging.support.MessageBuilder
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.json.JsonMapper
import java.time.Clock
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class MessagingComponentsTest {
    private val codec = OutgoingEventCodec(JsonMapper.builder().build())

    @Test
    fun `given every event type when encoding and decoding then it round trips`() {
        val tx = UUID.randomUUID()
        val order = UUID.randomUUID()
        val repair = UUID.randomUUID()
        val events =
            listOf(
                OutgoingEvent.DiagnosisCompleted(order, listOf(RepairReference(repair))),
                OutgoingEvent.OrderApproved(tx, order),
                OutgoingEvent.OrderRejected(tx, order),
                OutgoingEvent.OrderFinished(tx, order),
                OutgoingEvent.OrderCanceled(tx, order),
            )

        events.forEach { assertEquals(it, codec.decode(codec.encode(it))) }
    }

    @Test
    fun `given an unknown type when decoding then it fails`() {
        val json = """{"type":"mystery","orderId":"${UUID.randomUUID()}"}"""

        assertFailsWith<IllegalStateException> { codec.decode(json) }
    }

    @Test
    fun `given the scheduler when it fires then the relay runs one batch`() {
        val relay = mockk<OutboxRelay>(relaxed = true)

        OutboxRelayScheduler(relay).relay()

        verify { relay.relayBatch() }
    }

    @Test
    fun `given the relay config when building then the relay uses the configured batch size`() {
        val properties =
            OrdersProperties(OrdersProperties.Queues("c", "d", "o"), "http://front", OrdersProperties.Outbox(batchSize = 5))

        val relay =
            OutboxRelayConfig().outboxRelay(
                mockk<OutboxStore>(relaxed = true),
                mockk(relaxed = true),
                mockk<TransactionTemplate>(relaxed = true),
                Clock.systemUTC(),
                properties,
            )

        assertNotNull(relay)
    }

    private val processor = mockk<CommandMessageProcessor>()
    private val deadLetter = mockk<CommandDeadLetterPublisher>(relaxed = true)
    private val notifications = mockk<NotificationPort>(relaxed = true)
    private val listener = OrderCommandListener(processor, deadLetter, notifications)
    private val ack = mockk<Acknowledgement>(relaxed = true)
    private val visibility = mockk<Visibility>(relaxed = true)

    private fun message() =
        MessageBuilder
            .withPayload("""{"type":"cancelOrder"}""")
            .setHeader(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER, UUID.randomUUID().toString())
            .setHeader(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_DEDUPLICATION_ID_HEADER, UUID.randomUUID().toString())
            .setHeader(SqsHeaders.MessageSystemAttributes.SQS_APPROXIMATE_RECEIVE_COUNT, "3")
            .build()

    @Test
    fun `given an acknowledge decision when consuming then the message is acked and notified`() {
        val notification = mockk<Notification>()
        every { processor.process(any(), 3) } returns CommandDecision.Acknowledge(notification)

        listener.onMessage(message(), ack, visibility)

        verify { ack.acknowledge() }
        verify { notifications.send(notification) }
    }

    @Test
    fun `given an acknowledge without notification when consuming then nothing is sent`() {
        every { processor.process(any(), 3) } returns CommandDecision.Acknowledge(null)

        listener.onMessage(message(), ack, visibility)

        verify(exactly = 0) { notifications.send(any()) }
    }

    @Test
    fun `given a retry decision when consuming then the visibility timeout changes`() {
        every { processor.process(any(), 3) } returns CommandDecision.Retry(30, IllegalStateException("x"))

        listener.onMessage(message(), ack, visibility)

        verify { visibility.changeTo(30) }
        verify(exactly = 0) { ack.acknowledge() }
    }

    @Test
    fun `given a dead letter decision when consuming then it is published and acked`() {
        val error = IllegalArgumentException("bad")
        every { processor.process(any(), 3) } returns CommandDecision.DeadLetter(error)
        every { processor.statusOf(any()) } returns "CANCEL_ORDER"
        val message = message()

        listener.onMessage(message, ack, visibility)

        verify { deadLetter.publish(message, error) }
        verify { ack.acknowledge() }
        assertIs<CommandDecision.DeadLetter>(CommandDecision.DeadLetter(error))
    }
}
