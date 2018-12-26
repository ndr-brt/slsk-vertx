package ndr.brt.slsk

import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.EventBus
import org.slf4j.LoggerFactory

class ServerSocketHandler(private val eventBus: EventBus): Handler<Buffer> {

    private val log = LoggerFactory.getLogger(javaClass)
    private var rest = ProtocolBuffer()

    override fun handle(buffer: Buffer) {
        val message = rest.appendBuffer(buffer)

        if (message.size() + 4 > message.length()) {
            rest = message
            return
        } else {
            rest = ProtocolBuffer()
        }

        val type = message.code()
        when (type) {
            1 -> {
                log.info("Recv Login message")
                eventBus.publish("LoginResponded", LoginResponded(message.readByte().toInt() == 1, message.readString()).asJson())
            }
            else -> {
                log.warn("Server message code $type unknown")
                eventBus.publish("UnknownMessage", "{}")
            }
        }
    }
}