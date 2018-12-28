package ndr.brt.slsk

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.EventBus
import io.vertx.core.json.JsonObject
import io.vertx.core.net.NetClient
import io.vertx.core.net.NetSocket
import org.slf4j.LoggerFactory

class ServerListener(private val serverHost: String, private val serverPort: Int, server: NetClient, private val eventBus: EventBus, callback: (AsyncResult<ServerListener>) -> Unit) {
    private val log = LoggerFactory.getLogger(javaClass)

    init {
        log.info("Starting Server Listener")
        server.connect(serverPort, serverHost) {
            if (it.succeeded()) {
                log.info("Connected with server {}:{}", serverHost, serverPort)
                initialize(it.result())
                callback(Future.succeededFuture(this))
            } else {
                log.error("Connection with server failed", it.cause())
                callback(Future.failedFuture(it.cause()))
            }
        }
    }

    private fun initialize(socket: NetSocket) {
        socket.handler(InputMessageHandler("ServerInputMessage", eventBus))

        socket.exceptionHandler { cause ->
            log.error("Error", cause)
        }

        eventBus.consumer<Buffer>("ServerInputMessage", ServerInputMessage(eventBus))

        eventBus.consumer<JsonObject>("LoginRequested") { message ->
            val event = message.body().mapTo(LoginRequested::class.java)
            val login = Protocol.ToServer.Login(event.username, event.password)
            log.info("Send login message to server")
            socket.write(login.toChannel())
        }

        eventBus.consumer<JsonObject>("SearchRequested") { message ->
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
            val ip = inputMessage.readIp()
            val port = inputMessage.readInt()
            val token = inputMessage.readToken()
            log.info("Recv ConnectToPeer: username $username, type $type, ip $ip, port $port, token $token")
            eventBus.publish("ConnectToPeer", ConnectToPeer(username, type, ip.toString(), port, token).asJson())
        }

        private val login: (ProtocolBuffer) -> Unit = { inputMessage ->
            log.info("Recv Login")
            eventBus.publish("LoginResponded", LoginResponded(inputMessage.readByte().toInt() == 1, inputMessage.readString()).asJson())
        }

    }

    fun login(username: String, password: String, callback: (LoginResponded) -> Unit) {
        log.info("Login with username {}", username)

        eventBus.consumer<JsonObject>("LoginResponded") {
            callback(it.body().mapTo(LoginResponded::class.java))
        }

        emit(LoginRequested(username, password))
    }

    private fun emit(event: Event) {
        eventBus.publish(event::class.java.simpleName, event.asJson())
    }

}