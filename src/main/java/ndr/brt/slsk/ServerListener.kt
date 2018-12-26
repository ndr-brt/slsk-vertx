package ndr.brt.slsk

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.EventBus
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

        socket.exceptionHandler { cause ->
            log.error("Error", cause)
        }

        vertx.eventBus().consumer<Buffer>("ServerInputMessage", ServerInputMessage(vertx.eventBus()))

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

    class ServerInputMessage(private val eventBus: EventBus): Handler<io.vertx.core.eventbus.Message<Buffer>> {
        private val log = LoggerFactory.getLogger(javaClass)

        override fun handle(message: io.vertx.core.eventbus.Message<Buffer>) {
            val inputMessage = ProtocolBuffer(message.body())

            inputMessage.let(
                when (inputMessage.code()) {
                    1 -> login
                    18 -> connectToPeer
                    64 -> numberOfRooms
                    69 -> privilegedUsers
                    83 -> parentMinSpeed
                    84 -> parentSpeedRatio
                    104 -> wishListInterval
                    else -> unknownMessage
                }
            )
        }

        private val unknownMessage: (ProtocolBuffer) -> Unit = { inputMessage ->
            log.warn("Server message code ${inputMessage.code()} unknown")
        }

        private val wishListInterval: (ProtocolBuffer) -> Unit = { inputMessage ->
            val wishlistInterval = inputMessage.readInt()
            log.info("Recv WishlistInterval: $wishlistInterval")
        }

        private val parentSpeedRatio: (ProtocolBuffer) -> Unit = { inputMessage ->
            val parentSpeedRatio = inputMessage.readInt()
            log.info("Recv ParentSpeedRatio: $parentSpeedRatio")
        }

        private val parentMinSpeed: (ProtocolBuffer) -> Unit = { inputMessage ->
            val parentMinSpeed = inputMessage.readInt()
            log.info("Recv ParentMinSpeed: $parentMinSpeed")
        }

        private val privilegedUsers: (ProtocolBuffer) -> Unit = { inputMessage ->
            val privilegedUsers = inputMessage.readInt()
            log.info("Recv PrivilegedUsers: $privilegedUsers")
        }

        private val numberOfRooms: (ProtocolBuffer) -> Unit = { inputMessage ->
            val numberOfRooms = inputMessage.readInt()
            log.info("Recv RoomList: $numberOfRooms")
        }

        private val connectToPeer: (ProtocolBuffer) -> Unit = { inputMessage ->
            val username = inputMessage.readString()
            val type = inputMessage.readString()
            log.info("Recv ConnectToPeer: username $username, type $type")
        }

        private val login: (ProtocolBuffer) -> Unit = { inputMessage ->
            log.info("Recv Login")
            eventBus.publish("LoginResponded", LoginResponded(inputMessage.readByte().toInt() == 1, inputMessage.readString()).asJson())
        }

    }

}