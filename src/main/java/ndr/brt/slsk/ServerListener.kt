package ndr.brt.slsk

import io.vertx.core.eventbus.EventBus
import io.vertx.core.json.JsonObject
import io.vertx.core.net.NetSocket
import org.slf4j.LoggerFactory

class ServerListener(private val socket: NetSocket, private val eventBus: EventBus) {
    private val log = LoggerFactory.getLogger(javaClass)

    init {
        socket.handler  { buffer ->
            log.info("Received message from server: {}", buffer)
            log.info("Received message from server: {}", buffer.bytes)
            val status = buffer.getByte(8)
            log.info("Status {}", status)
            eventBus.publish("login", JsonObject.mapFrom(Login(status.toInt() == 1, buffer.toString())))
        }

        eventBus.consumer<JsonObject>("do-login") { message ->
            val username = message.body().getString("username")
            val password = message.body().getString("password")
            val login = Protocol.ToServer.Login(username, password)
            log.info("Send login message to server {}", socket.localAddress())
            socket.write(login.toChannel())
        }
    }
}