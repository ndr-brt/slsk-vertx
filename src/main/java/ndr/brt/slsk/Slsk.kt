package ndr.brt.slsk

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory

fun main(args: Array<String>) {
    val slsk = Slsk("aaaa.slsknet.org", 2242)
    Vertx.vertx().deployVerticle(slsk) {
        slsk.login("aaaaa", "bbbb")
    }
}

class Slsk(private val serverHost: String, private val serverPort: Int): AbstractVerticle() {

    val log = LoggerFactory.getLogger(javaClass)

    override fun start(startFuture: Future<Void>) {
        log.info("Starting slsk verticle")
        vertx.createNetClient().connect(serverPort, serverHost) {
            if (it.succeeded()) {
                log.info("Connected with server {}:{}", serverHost, serverPort)
                ServerListener(it.result(), vertx.eventBus())
                startFuture.complete()
            } else {
                log.error("Connection with server failed", it.cause())
                startFuture.fail(it.cause())
            }
        }
    }

    fun login(username: String, password: String) {
        log.info("Login with username {}", username)
        vertx.eventBus().send("do-login", JsonObject().put("username", username).put("password", password))
    }

}
