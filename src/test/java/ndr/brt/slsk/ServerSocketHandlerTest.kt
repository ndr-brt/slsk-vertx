package ndr.brt.slsk

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.EventBus
import org.junit.jupiter.api.Test

internal class ServerSocketHandlerTest {

    private val eventBus = mock<EventBus>()
    private val handler = ServerSocketHandler(eventBus)

    @Test
    internal fun read_one_message() {
        val message = Protocol.FromServer.Login(true, "goood!")

        handler.handle(message.toChannel())

        verify(eventBus).publish(any(), any())
    }

    @Test
    internal fun read_messages_in_two_parts() {
        val partOne = Buffer.buffer()
                .appendIntLE(17)
                .appendIntLE(1)
                .appendByte(1)
                .appendIntLE(8)

        val partTwo = Buffer.buffer()
                .appendString("12345678")

        handler.handle(partOne)
        handler.handle(partTwo)

        verify(eventBus).publish(any(), any())
    }
}