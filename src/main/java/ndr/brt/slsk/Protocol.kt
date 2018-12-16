package ndr.brt.slsk

import io.vertx.core.buffer.Buffer


class Protocol {
    class FromServer {
        class Login(private val successful: Boolean, private val message: String): Message {
            override fun toBuffer() = Buffer.buffer()
                    .appendInt(1)
                    .appendByte(if (successful) 1 else 0)
                    .appendString(message)
        }
    }
}

interface Message {
    fun toBuffer(): Buffer
    fun toChannel(): Buffer {
        val buffer = toBuffer()
        return Buffer.buffer()
                .appendIntLE(buffer.length())
                .appendBuffer(buffer)
    }
}