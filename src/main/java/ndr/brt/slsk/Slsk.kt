package ndr.brt.slsk

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.core.net.NetSocket
import org.slf4j.LoggerFactory

class Slsk(private val serverHost: String, private val serverPort: Int): AbstractVerticle() {

    val log = LoggerFactory.getLogger(javaClass)

    override fun start(startFuture: Future<Void>) {
        vertx.createNetClient().connect(serverPort, serverHost) {
            if (it.succeeded()) {
                vertx.deployVerticle(ServerListener(it.result())) {
                    startFuture.complete()

                }
            }
        }
    }

    fun login(username: String, password: String) {
        log.info("Login with username {}", username)
        vertx.eventBus().send("do-login", JsonObject().put("username", username).put("password", password))
    }

    class ServerListener(private val socket: NetSocket): AbstractVerticle() {
        val log = LoggerFactory.getLogger(javaClass)

        override fun start() {
            socket.handler  {
                buffer ->
                log.info("Received message from server: {}", buffer)
                log.info("Received message from server: {}", buffer.bytes)
                val status = buffer.getByte(8)
                log.info("Status {}", status)
                vertx.eventBus().publish("login", JsonObject.mapFrom(Login(status.toInt() == 1, buffer.toString())))
            }

            vertx.eventBus().consumer<JsonObject>("do-login") {
                message -> socket.write(message.body().getString("username"))
            }
        }
    }

}
