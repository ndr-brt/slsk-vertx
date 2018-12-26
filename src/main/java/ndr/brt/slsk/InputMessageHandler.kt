package ndr.brt.slsk

import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.EventBus

class InputMessageHandler(private val publishAddress: String, private val eventBus: EventBus) : Handler<Buffer> {

    private var rest = Buffer.buffer()

    override fun handle(buffer: Buffer) {
        val message = rest.appendBuffer(buffer)
        val size = message.getIntLE(0)

        rest = if (size + 4 > message.length()) {
            message
        } else {
            eventBus.publish(publishAddress, message)
            Buffer.buffer()
        }

    }

}