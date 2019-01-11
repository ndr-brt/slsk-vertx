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
                    .appendIntLE(message.length)
                    .appendString(message)
        }
    }

    class ToServer {
        class Login(private val username: String, val password: String) : SlskMessage {
            override fun toBuffer(): Buffer = Buffer.buffer()
                    .appendIntLE(1)
                    .appendIntLE(username.length)
                    .appendString(username)
                    .appendIntLE(password.length)
                    .appendString(password)
                    .appendIntLE(160)
                    .appendIntLE(username.plus(password).let(md5).let(bytesToHex).length)
                    .appendString(username.plus(password).let(md5).let(bytesToHex))
                    .appendIntLE(17)
        }

        class FileSearch(private val token: String, private val query: String): SlskMessage {
            override fun toBuffer(): Buffer = Buffer.buffer()
                    .appendIntLE(26)
                    .appendBytes(token.let(hexToBytes))
                    .appendIntLE(query.length)
                    .appendString(query)
        }
    }

    class ToPeer {
        class PierceFirewall(private val token: String): SlskMessage {
            override fun toBuffer(): Buffer = Buffer.buffer()
                    .appendUnsignedByte(0)
                    .appendBytes(token.let(hexToBytes))
        }

        class TransferRequest(private val token: String, private val filename: String): SlskMessage {
            override fun toBuffer(): Buffer = Buffer.buffer()
                    .appendIntLE(40)
                    .appendIntLE(0)
                    .appendBytes(token.let(hexToBytes))
                    .appendIntLE(filename.length)
                    .appendString(filename)
        }

        class TransferResponse(private val token: String): SlskMessage {
            override fun toBuffer(): Buffer = Buffer.buffer()
                    .appendIntLE(41)
                    .appendBytes(token.let(hexToBytes))
                    .appendByte(1)
        }
    }
}

