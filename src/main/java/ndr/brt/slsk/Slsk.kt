package ndr.brt.slsk

import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory

class Slsk(private val serverHost: String, private val serverPort: Int): AbstractVerticle() {

    val log = LoggerFactory.getLogger(javaClass)

    fun login(username: String, password: String) {
        log.info("Login with username {}", username)
        vertx.createNetClient().connect(serverPort, serverHost) {
            if (it.succeeded()) {
                it.result().handler {
                    buffer ->
                        log.info("Received message from server: {}", buffer)
                        log.info("Received message from server: {}", buffer.bytes)
                        val status = buffer.getByte(12)
                        log.info("Status {}", status)
                        vertx.eventBus().publish("login", JsonObject.mapFrom(Login(status.toInt() == 1, buffer.toString())))
                }
                it.result().write(username)
            }
        }
    }

}
