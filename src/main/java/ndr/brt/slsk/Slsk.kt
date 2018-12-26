package ndr.brt.slsk

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Future.future
import io.vertx.core.Vertx
import org.slf4j.LoggerFactory

fun main(args: Array<String>) {
    val slsk = Slsk("server.slsknet.org", 2242)
    val vertx = Vertx.vertx()
    vertx.deployVerticle(slsk) {
        if (it.failed()) throw it.cause()

        slsk.login("ginogino", "ginogino")
        vertx.setTimer(2000) {
            slsk.search("leatherface", 2000)

        }
    }
}

class Slsk(private val serverHost: String, private val serverPort: Int): AbstractVerticle() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun start(startFuture: Future<Void>) {
        log.info("Starting slsk verticle")
        val serverListener = future<String>()
        vertx.deployVerticle(ServerListener(serverHost, serverPort), serverListener::handle)

        serverListener.setHandler {
            if (it.failed()) startFuture.fail(it.cause())

            log.info("Slsk verticle started")
            startFuture.complete()
        }
    }

    fun login(username: String, password: String) {
        log.info("Login with username {}", username)
        emit(LoginRequested(username, password))
    }

    fun search(query: String, timeout: Long) {
        log.info("Search request {} with timeout {}", query, timeout)
        val token = "01a2f123"
        emit(SearchRequested(query, token))
        vertx.setTimer(timeout) {
            emit(SearchResponded(emptyList()))
        }
    }

    fun emit(event: Event) {
        vertx.eventBus().publish(event::class.java.simpleName, event.asJson())
    }

}
