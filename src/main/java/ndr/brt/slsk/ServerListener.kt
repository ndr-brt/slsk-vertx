package ndr.brt.slsk

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.core.net.NetSocket
import org.slf4j.LoggerFactory

class ServerListener(private val serverHost: String, private val serverPort: Int) : AbstractVerticle() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun start(startFuture: Future<Void>) {
        log.info("Starting slsk verticle")
        vertx.createNetClient().connect(serverPort, serverHost) {
            if (it.succeeded()) {
                log.info("Connected with server {}:{}", serverHost, serverPort)
                initialize(it.result())
                startFuture.complete()
            } else {
                log.error("Connection with server failed", it.cause())
                startFuture.fail(it.cause())
            }
        }
    }

    private fun initialize(socket: NetSocket) {
        socket.handler { buffer ->
            log.info("Received message from server: {}", buffer)
            val status = buffer.getByte(8)
            log.info("Status {}", status)
            vertx.eventBus().publish("LoginResponded", LoginResponded(status.toInt() == 1, buffer.toString()).asJson())
        }

        vertx.eventBus().consumer<JsonObject>("LoginRequested") { message ->
            val event = message.body().mapTo(LoginRequested::class.java)
            val login = Protocol.ToServer.Login(event.username, event.password)
            log.info("Send login message to server")
            socket.write(login.toChannel())
        }

        vertx.eventBus().consumer<JsonObject>("SearchRequested") { message ->
            val event = message.body().mapTo(SearchRequested::class.java)
            val fileSearch = Protocol.ToServer.FileSearch(event.token, event.query)
            log.info("Send file search message to server")
            socket.write(fileSearch.toChannel())
        }
    }

}