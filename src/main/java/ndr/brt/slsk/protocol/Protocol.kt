package ndr.brt.slsk.protocol

import bytesToHex
import hexToBytes
import io.vertx.core.buffer.Buffer
import ndr.brt.slsk.md5

class Protocol {
    class FromServer {
        class Login(private val successful: Boolean, private val message: String): SlskMessage {
            override fun toBuffer(): Buffer = Buffer.buffer()
                    .appendIntLE(1)
                    .appendByte(if (successful) 1 else 0)
                    .appendStringWithLength(message)
        }
    }

    class ToServer {
        class Login(private val username: String, val password: String) : SlskMessage {
            override fun toBuffer(): Buffer = Buffer.buffer()
                    .appendIntLE(1)
                    .appendStringWithLength(username)
                    .appendStringWithLength(password)
                    .appendIntLE(160)
                    .appendStringWithLength(username.plus(password).let(md5).let(bytesToHex))
                    .appendIntLE(17)
        }

        class FileSearch(private val token: String, private val query: String): SlskMessage {
            override fun toBuffer(): Buffer = Buffer.buffer()
                    .appendIntLE(26)
                    .appendBytes(token.let(hexToBytes))
                    .appendStringWithLength(query)
        }
    }

    class ToPeer {
        class PierceFirewall(private val token: String): SlskMessage {
            override fun toBuffer(): Buffer = Buffer.buffer()
                    .appendUnsignedByte(0)
                    .appendBytes(token.let(hexToBytes))
        }

        class PeerInit(private val username: String, private val type: String, private val token: String): SlskMessage {
            override fun toBuffer(): Buffer = Buffer.buffer()
                    .appendUnsignedByte(1)
                    .appendStringWithLength(username)
                    .appendStringWithLength(type)
                    .appendBytes(token.let(hexToBytes))

        }

        class TransferRequest(private val token: String, private val filename: String): SlskMessage {
            override fun toBuffer(): Buffer = Buffer.buffer()
                    .appendIntLE(40)
                    .appendIntLE(0)
                    .appendBytes(token.let(hexToBytes))
                    .appendStringWithLength(filename)
        }

        class TransferResponse(private val token: String): SlskMessage {
            override fun toBuffer(): Buffer = Buffer.buffer()
                    .appendIntLE(41)
                    .appendBytes(token.let(hexToBytes))
                    .appendUnsignedByte(1)
        }
    }
}

fun Buffer.appendStringWithLength(value: String) : Buffer = this
    .appendIntLE(value.length)
    .appendString(value)
