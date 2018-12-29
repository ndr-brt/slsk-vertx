package ndr.brt.slsk

import bytesToHex
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.eventbus.EventBus
import io.vertx.core.json.JsonObject
import ndr.brt.slsk.peer.PeerListener
import ndr.brt.slsk.server.ServerListener
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
    private val searchResults: MutableMap<String, MutableList<String>> = mutableMapOf()

    override fun start(startFuture: Future<Void>) {
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

        vertx.eventBus().on(ConnectToPeer::class) { event ->
            PeerListener(event.address, event.info, vertx.createNetClient(), vertx.eventBus()) { async ->
                if (async.failed()) {
                    log.error("Error connecting to ${event.info.username} on ${event.address}: ${async.cause()}")
                } else {
                    log.info("Connected to ${event.info.username} on ${event.address}")
                }
            }
        }
    }

    fun search(query: String, timeout: Long, callback: (SearchResponded) -> Unit) {
        val token = nextBytes(4).let(bytesToHex)
        log.info("Search request $query with timeout $timeout and token $token")
        vertx.eventBus().emit(SearchRequested(query, token))

        searchResults[token] = mutableListOf()
        vertx.eventBus().on(SearchResponded::class) { event ->
            searchResults[token]!!.addAll(event.files)
        }

        vertx.setTimer(timeout) {
            callback(SearchResponded(token, searchResults[token].orEmpty()))
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
