package ndr.brt.slsk

import io.vertx.core.buffer.Buffer
import java.security.MessageDigest


class Protocol {
    class FromServer {
        class Login(private val successful: Boolean, private val message: String): Message {
            override fun type(): Int = 1

            override fun toBuffer() = Buffer.buffer()
                    .appendIntLE(type())
                    .appendByte(if (successful) 1 else 0)
                    .appendIntLE(message.length)
                    .appendString(message)
        }
    }

    class ToServer {
        class Login(private val username: String, val password: String): Message {

            override fun type(): Int = 1

            override fun toBuffer(): Buffer = Buffer.buffer()
                    .appendIntLE(type())
                    .appendIntLE(username.length)
                    .appendString(username)
                    .appendIntLE(password.length)
                    .appendString(password)
                    .appendIntLE(160)
                    .appendIntLE(username.plus(password).let(md5).let(hex).length)
                    .appendString(username.plus(password).let(md5).let(hex))
                    .appendIntLE(17)
        }
    }
}

val md5: (String) -> ByteArray = { string ->
    MessageDigest.getInstance("MD5").digest(string.toByteArray())
}

val hex: (ByteArray) -> String = { bytes ->
    bytes.joinToString { String.format("%02X", it) }
}

val parseToServerMessage: (Buffer) -> Message = {
    val buffer = ProtocolBuffer(it)
    val type = buffer.type()
    when (type) {
        1 -> Protocol.ToServer.Login(buffer.readString(), buffer.readString())
        else -> UnknownMessage(type)
    }
}

class UnknownMessage(private val type: Int) : Message {
    override fun toBuffer(): Buffer {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun type(): Int = type

}

interface Message {
    fun toBuffer(): Buffer
    fun toChannel(): Buffer {
        val buffer = toBuffer()
        return Buffer.buffer()
                .appendIntLE(buffer.length())
                .appendBuffer(buffer)
    }

    fun type(): Int
}
