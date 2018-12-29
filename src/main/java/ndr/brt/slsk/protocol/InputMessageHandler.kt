package ndr.brt.slsk.protocol

import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.EventBus

class InputMessageHandler(private val publishAddress: String, private val eventBus: EventBus) : Handler<Buffer> {

    private var rest = byteArrayOf()

    override fun handle(buffer: Buffer) {
        val message = Buffer.buffer(rest).appendBuffer(buffer)
        val size = message.getIntLE(0)

        rest = if (size + 4 <= message.length()) {
            eventBus.send(publishAddress, message.slice(0, size + 4))
            message.slice(size + 4, message.length()).bytes
        } else {
            message.slice(0, message.length()).bytes
        }
    }

}