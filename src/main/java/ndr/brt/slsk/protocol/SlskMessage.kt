package ndr.brt.slsk.protocol

import io.vertx.core.buffer.Buffer

interface SlskMessage {
    fun toBuffer(): Buffer
    fun toChannel(): Buffer {
        val buffer = toBuffer()
        return Buffer.buffer()
                .appendIntLE(buffer.length())
                .appendBuffer(buffer)
    }
}