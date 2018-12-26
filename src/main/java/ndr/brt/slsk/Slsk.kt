package ndr.brt.slsk

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Future.future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory

fun main(args: Array<String>) {
    val slsk = Slsk("ginogino", "ginogino", "server.slsknet.org", 2242)
    val vertx = Vertx.vertx()
    vertx.deployVerticle(slsk) {
        if (it.failed()) throw it.cause()

        slsk.search("leatherface", 2000)
    }
}

class Slsk(private val username: String, private val password: String, private val serverHost: String, private val serverPort: Int): AbstractVerticle() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun start(startFuture: Future<Void>) {
        log.info("Starting slsk verticle")
        val serverListener = future<String>()
        vertx.deployVerticle(ServerListener(serverHost, serverPort), serverListener::handle)

        serverListener.setHandler {
            if (it.failed()) startFuture.fail(it.cause())

            log.info("Slsk verticle started")
            login(username, password)

            vertx.eventBus().consumer<JsonObject>("LoginResponded") {
                val login = it.body().mapTo(LoginResponded::class.java)
                if (login.succeed) {
                    log.info("Login succedeed: ${login.message}")
                    startFuture.complete()
                } else {
                    log.info("Login failed: ${login.message}")
                    startFuture.fail(login.message)
                }
            }
        }
    }

    fun search(query: String, timeout: Long) {
        log.info("Search request {} with timeout {}", query, timeout)
        val token = "01a2f123"
        emit(SearchRequested(query, token))
        vertx.setTimer(timeout) {
            emit(SearchResponded(emptyList()))
        }
    }

    private fun login(username: String, password: String) {
        log.info("Login with username {}", username)
        emit(LoginRequested(username, password))
    }

    private fun emit(event: Event) {
        vertx.eventBus().publish(event::class.java.simpleName, event.asJson())
    }

}
