package ndr.brt.slsk

import bytesToHex
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Future.future
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.EventBus
import io.vertx.core.json.JsonObject
import io.vertx.core.net.NetSocket
import org.slf4j.LoggerFactory
import kotlin.random.Random.Default.nextBytes
import kotlin.reflect.KClass

fun main(args: Array<String>) {
    val slsk = Slsk("ginogino", "ginogino", "server.slsknet.org", 2242)
    val vertx = Vertx.vertx()
    vertx.deployVerticle(slsk) {
        if (it.failed()) throw it.cause()

        slsk.search("leatherface", 2000) { result ->
            println(result.files)
        }
    }
}

class Slsk(private val username: String, private val password: String, private val serverHost: String, private val serverPort: Int): AbstractVerticle() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun start(startFuture: Future<Void>) {
        log.info("Starting slsk verticle")

        ServerListener(serverHost, serverPort, vertx.createNetClient(), vertx.eventBus()) { async ->
            if (async.failed()) startFuture.fail(async.cause())

            val server = async.result()
            server.login(username, password) { login ->
                if (login.succeed) {
                    log.info("Login succedeed: ${login.message}")
                    startFuture.complete()
                } else {
                    log.info("Login failed: ${login.message}")
                    startFuture.fail(login.message)
                }
            }
        }

        vertx.eventBus().consumer<JsonObject>("ConnectToPeer") { message ->
            val event = message.body().mapTo(ConnectToPeer::class.java)
            val peerListener = future<NetSocket>()
            val client = vertx.createNetClient()
            client.connect(event.port, event.host, peerListener::handle)

            peerListener.setHandler {
                if (it.failed()) {
                    log.error("Error connecting to ${event.username}", it.cause())
                } else {
                    val socket = it.result()
                    socket.handler(InputMessageHandler("PeerInputMessage-${event.username}", vertx.eventBus()))
                    vertx.eventBus().consumer<Buffer>("PeerInputMessage-${event.username}") { message ->
                        val inputMessage = ProtocolBuffer(message.body())
                        log.info("Received message from user ${event.username} code ${inputMessage.code()}")
                        when (inputMessage.code()) {
                            9 -> {
                                val unzip = inputMessage.decompress()
                                val username = unzip.readString()
                                val token = unzip.readToken()
                                val resultsCount = unzip.readInt()
                                val fileResult = mutableListOf<String>()
                                for (i in 0 until resultsCount) {
                                    unzip.readByte()
                                    val filename = unzip.readString()
                                    val size1 = unzip.readInt()
                                    val size2 = unzip.readInt()
                                    unzip.readString()
                                    val attributesCount = unzip.readInt()
                                    for (j in 0 until attributesCount) {
                                        unzip.readInt()
                                        unzip.readInt()
                                    }
                                    fileResult.add(filename)
                                }
                                unzip.readByte()
                                unzip.readInt()
                                unzip.readInt()
                                log.info("Recv FileSearchResult da $username")
                                if (fileResult.size > 0) {
                                    vertx.eventBus().emit(SearchResponded(fileResult))
                                }
                            }
                            else -> log.warn("Peer message unknown: ${inputMessage.code()}")
                        }
                    }

                    socket.write(Protocol.ToPeer.PierceFirewall(event.token).toChannel())
                }

            }
        }
    }

    fun search(query: String, timeout: Long, callback: (SearchResponded) -> Unit) {
        val token = nextBytes(4).let(bytesToHex)
        log.info("Search request $query with timeout $timeout and token $token")
        vertx.eventBus().emit(SearchRequested(query, token))

        val results = mutableListOf<String>()
        vertx.eventBus().on(SearchResponded::class) { event ->
            results.addAll(event.files)
        }

        vertx.setTimer(timeout) {
            callback(SearchResponded(results))
        }
    }

}

fun EventBus.emit(event: Event) {
    publish(event::class.java.simpleName, JsonObject.mapFrom(event))
}

fun <T> EventBus.on(clazz: KClass<T>, function: (T) -> Unit) where T: Event  {
    consumer<JsonObject>(clazz::java.get().simpleName) { message ->
        function.invoke(message.body().mapTo(clazz::java.get()))
    }
}
