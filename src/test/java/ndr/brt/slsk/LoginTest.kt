package ndr.brt.slsk

import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import io.vertx.core.net.NetSocket
import io.vertx.junit5.Timeout
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.TimeUnit.SECONDS

@ExtendWith(VertxExtension::class)
class LoginTest {

    @Test
    @Timeout(value = 2, timeUnit = SECONDS)
    internal fun do_login_emits_login_successful_event(vertx: Vertx, context: VertxTestContext) {
        mockServer(vertx)
        var slsk = Slsk("localhost", 4321)

        vertx.deployVerticle(slsk)

        vertx.eventBus().consumer<JsonObject>("login") {
            val login = it.body().mapTo(Login::class.java)
            assertThat(login.succeed).isTrue()
            assertThat(login.message).endsWith("Welcome to soulseek!")
            context.completeNow()
        }

        slsk.login("username", "password")
    }

    @Test
    @Timeout(value = 2, timeUnit = SECONDS)
    internal fun when_login_is_not_valid_emit_login_failed_event(vertx: Vertx, context: VertxTestContext) {
        mockServer(vertx)
        var slsk = Slsk("localhost", 4321)

        vertx.deployVerticle(slsk)
        vertx.eventBus().consumer<JsonObject>("login") {
            val login = it.body().mapTo(Login::class.java)
            assertThat(login.succeed).isFalse()
            assertThat(login.message).endsWith("Login failed!")
            context.completeNow()
        }

        slsk.login("user_unknown", "password")
    }

    private fun mockServer(vertx: Vertx) {
        val connectionHandler: (NetSocket) -> Unit = { socket ->
            val socketHandler: (Buffer) -> Unit = { buffer ->
                println("Received message: $buffer")
                val message = when {
                    buffer.toString().contains("username") -> Buffer.buffer()
                            .appendIntLE(15)
                            .appendInt(1)
                            .appendByte(1)
                            .appendString("Welcome to soulseek!")
                    else -> Buffer.buffer()
                            .appendIntLE(15)
                            .appendInt(1)
                            .appendByte(0)
                            .appendString("Login failed!")
                }
                socket.write(Buffer.buffer()
                        .appendIntLE(message.length())
                        .appendBuffer(message))
            }

            socket.handler(socketHandler)
        }

        vertx.createNetServer()
                .connectHandler(connectionHandler)
                .listen(4321)
    }
}