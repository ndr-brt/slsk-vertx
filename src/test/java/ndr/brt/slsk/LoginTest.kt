package ndr.brt.slsk

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.Timeout
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit.SECONDS

@ExtendWith(VertxExtension::class)
class LoginTest {

    @BeforeEach
    internal fun setUp(vertx: Vertx) {
        val future = CompletableFuture<Void>()
        vertx.deployVerticle(MockServer(4321)) {
            future.complete(null)
        }
        future.get()
    }

    @Test
    @Timeout(value = 5, timeUnit = SECONDS)
    internal fun do_login_emits_login_successful_event(vertx: Vertx, context: VertxTestContext) {
        val slsk = Slsk("localhost", 4321)
        vertx.deployVerticle(slsk) {
            slsk.login("username", "password")

            vertx.eventBus().consumer<JsonObject>("LoginResponded") {
                val login = it.body().mapTo(LoginResponded::class.java)
                assertThat(login.succeed).isTrue()
                assertThat(login.message).endsWith("Welcome to soulseek!")
                context.completeNow()
            }

        }
    }

    @Test
    @Timeout(value = 5, timeUnit = SECONDS)
    internal fun when_login_is_not_valid_emit_login_failed_event(vertx: Vertx, context: VertxTestContext) {
        val slsk = Slsk("localhost", 4321)
        vertx.deployVerticle(slsk) {
            slsk.login("username", "wrong_password")

            vertx.eventBus().consumer<JsonObject>("LoginResponded") {
                val login = it.body().mapTo(LoginResponded::class.java)
                assertThat(login.succeed).isFalse()
                assertThat(login.message).endsWith("Login failed!")
                context.completeNow()
            }
        }
    }

    @Test
    @Timeout(value = 5, timeUnit = SECONDS)
    internal fun when_server_is_not_reachable_deploy_verticle_will_fail(vertx: Vertx, context: VertxTestContext) {
        val slsk = Slsk("localhost", 4322)
        vertx.deployVerticle(slsk) {
            assertThat(it.failed()).isTrue()
            context.completeNow()
        }
    }

}