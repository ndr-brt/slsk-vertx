package ndr.brt.slsk.protocol

import bytesToHex
import io.vertx.core.buffer.Buffer
import java.io.ByteArrayInputStream
import java.util.zip.InflaterInputStream

class ProtocolBuffer(private val buffer: Buffer, private var pointer: Int = 8) {

    fun code(): Int = buffer.getIntLE(4)

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

    fun readIp(): Ip {
        return Ip(
                buffer.getUnsignedByte(pointer++),
                buffer.getUnsignedByte(pointer++),
                buffer.getUnsignedByte(pointer++),
                buffer.getUnsignedByte(pointer++)
        )
    }

    fun readToken(): String {
        val bytes = buffer.getBytes(pointer, pointer + 4)
        pointer += 4
        return bytesToHex(bytes)
    }

    fun decompress(): ProtocolBuffer = buffer.getBytes(pointer, buffer.length())
        .let(::ByteArrayInputStream)
        .let(::InflaterInputStream)
        .use { stream ->
            val readAllBytes = stream.readAllBytes()
            ProtocolBuffer(Buffer.buffer(readAllBytes), 0)
        }

    fun readBoolean(): Boolean = readByte().toInt() == 1

    fun readTransferDirection(): TransferDirection = TransferDirection.values()[readInt()]

}