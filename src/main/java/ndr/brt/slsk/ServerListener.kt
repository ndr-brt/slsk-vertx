package ndr.brt.slsk

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import io.vertx.core.net.NetSocket
import org.slf4j.LoggerFactory

class ServerListener(private val serverHost: String, private val serverPort: Int) : AbstractVerticle() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun start(startFuture: Future<Void>) {
        log.info("Starting Server Listener")
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
        socket.handler(InputMessageHandler("ServerInputMessage", vertx.eventBus()))

        vertx.eventBus().consumer<Buffer>("ServerInputMessage") { message ->
            val inputMessage = ProtocolBuffer(message.body())

            val type = inputMessage.code()
            when (type) {
                1 -> {
                    log.info("Recv Login message")
                    vertx.eventBus().publish("LoginResponded", LoginResponded(inputMessage.readByte().toInt() == 1, inputMessage.readString()).asJson())
                }
                else -> {
                    log.warn("Server message code $type unknown")
                    vertx.eventBus().publish("UnknownMessage", "{}")
                }
            }

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