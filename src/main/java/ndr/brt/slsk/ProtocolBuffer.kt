package ndr.brt.slsk

import io.vertx.core.buffer.Buffer

class ProtocolBuffer(private val buffer: Buffer) {
    var pointer = 8
    fun type(): Int = buffer.getIntLE(4)

    fun readString(): String {
        val length = readInt()
        val string = buffer.getString(pointer, pointer + length)
        pointer += length
        return string
    }

    fun readInt(): Int {
        val int = buffer.getIntLE(pointer)
        pointer += 4
        return int
    }

}