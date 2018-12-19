package ndr.brt.slsk

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Future.future
import io.vertx.core.Vertx
import org.slf4j.LoggerFactory

fun main(args: Array<String>) {
    val slsk = Slsk("aaaa.slsknet.org", 2242)
    Vertx.vertx().deployVerticle(slsk) {
        if (it.failed()) throw it.cause()

        slsk.login("aaaaa", "bbbb")
    }
}

class Slsk(private val serverHost: String, private val serverPort: Int): AbstractVerticle() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun start(startFuture: Future<Void>) {
        log.info("Starting slsk verticle")
        val serverListener = future<String>()
        vertx.deployVerticle(ServerListener(serverHost, serverPort)) {
            log.info("Slsk verticle co")
            serverListener.handle(it)
        }

        serverListener.setHandler {
            if (it.failed()) startFuture.fail(it.cause())

            startFuture.complete()
        }
    }

    fun login(username: String, password: String) {
        log.info("Login with username {}", username)
        vertx.eventBus().publish("LoginRequested", LoginRequested(username, password).asJson())
    }

    fun search(query: String, timeout: Long) {
        log.info("Search request {} with timeout {}", query, timeout)
        val token = "01a2f123"
        vertx.eventBus().publish("SearchRequested", SearchRequested(query, token).asJson())
        vertx.setTimer(timeout) {
            vertx.eventBus().publish("SearchResponded", SearchResponded(emptyList()).asJson())
        }
    }

}
