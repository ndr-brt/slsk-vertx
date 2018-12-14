package ndr.brt.slsk

import io.vertx.core.AbstractVerticle

class Slsk(private val serverHost: String, private val serverPort: Int): AbstractVerticle() {

    fun login(username: String, password: String) {
        vertx.createNetClient().connect(serverPort, serverHost) {
            if (it.succeeded()) {
                it.result().handler {
                    buffer -> vertx.eventBus().publish("login", buffer.toString())
                }
                it.result().write("any")
            }
        }
    }

}
