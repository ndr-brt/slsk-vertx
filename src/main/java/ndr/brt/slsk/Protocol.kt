package ndr.brt.slsk

import io.vertx.core.buffer.Buffer
import java.security.MessageDigest


class Protocol {
    class FromServer {
        class Login(private val successful: Boolean, private val message: String): Message {
            override fun toBuffer() = Buffer.buffer()
                    .appendInt(1)
                    .appendByte(if (successful) 1 else 0)
                    .appendString(message)
        }
    }

    class ToServer {
        class Login(private val username: String, private val password: String): Message {
            override fun toBuffer(): Buffer = Buffer.buffer()
                    .appendInt(1)
                    .appendString(username)
                    .appendString(password)
                    .appendInt(160)
                    .appendString(username.plus(password).let(md5).let(hex))
                    .appendInt(17)
        }
    }
}

val md5: (String) -> ByteArray = { string ->
    MessageDigest.getInstance("MD5").digest(string.toByteArray())
}

val hex: (ByteArray) -> String = { bytes ->
    bytes.joinToString { String.format("%02X", it) }
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
