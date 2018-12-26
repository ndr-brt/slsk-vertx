package ndr.brt.slsk

import io.vertx.core.buffer.Buffer

class ProtocolBuffer(private val buffer: Buffer) {
    constructor() : this(Buffer.buffer())

    var pointer = 8

    fun size(): Int = buffer.getIntLE(0)
    fun type(): Int = buffer.getIntLE(4)
    fun length(): Int = buffer.length()

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

    fun readByte(): Byte {
        val byte = buffer.getByte(pointer)
        pointer += 1
        return byte
    }

    fun appendBuffer(buff: Buffer): ProtocolBuffer = ProtocolBuffer(buffer.appendBuffer(buff))

}