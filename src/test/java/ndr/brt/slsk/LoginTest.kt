package ndr.brt.slsk

import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.junit5.Timeout
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.TimeUnit.SECONDS

@ExtendWith(VertxExtension::class)
class LoginTest {

    @Test
    @Timeout(value = 2, timeUnit = SECONDS)
    internal fun do_login_emits_login_successful_event(vertx: Vertx, context: VertxTestContext) {
        vertx.createNetServer()
                .connectHandler {
                    socket -> socket.handler {
                        buffer ->
                            val message = Buffer.buffer()
                                .appendIntLE(15)
                                .appendInt(1)
                                .appendByte(1)
                                .appendString("Welcome to soulseek!")
                            socket.write(Buffer.buffer()
                                .appendIntLE(message.length())
                                .appendBuffer(message))
                    }
                }
                .listen(4321)

        val slsk = Slsk("localhost", 4321)

        vertx.deployVerticle(slsk)

        vertx.eventBus().consumer<String>("login") {
            assertThat(it.body()).endsWith("Welcome to soulseek!")
            context.completeNow()
        }

        slsk.login("username", "password")
    }
}