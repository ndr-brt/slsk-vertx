package ndr.brt.slsk

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory

class Slsk(private val serverHost: String, private val serverPort: Int): AbstractVerticle() {

    val log = LoggerFactory.getLogger(javaClass)

    override fun start(startFuture: Future<Void>) {
        vertx.createNetClient().connect(serverPort, serverHost) {
            if (it.succeeded()) {
                ServerListener(it.result(), vertx.eventBus())
                startFuture.complete()
            }
        }
    }

    fun login(username: String, password: String) {
        log.info("Login with username {}", username)
        vertx.eventBus().send("do-login", JsonObject().put("username", username).put("password", password))
    }

}
