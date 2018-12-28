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
class SlskTest {

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
    internal fun when_server_is_not_reachable_deploy_fails(vertx: Vertx, context: VertxTestContext) {
        val slsk = Slsk("any", "any", "localhost", 4322)
        vertx.deployVerticle(slsk) {
            assertThat(it.failed()).isTrue()
            assertThat(it.cause().message).isEqualTo("Connection refused: localhost/127.0.0.1:4322")
            context.completeNow()
        }
    }

    @Test
    @Timeout(value = 5, timeUnit = SECONDS)
    internal fun when_credentials_are_not_valid_deploy_fails(vertx: Vertx, context: VertxTestContext) {
        val slsk = Slsk("username", "wrong_password", "localhost", 4321)
        vertx.deployVerticle(slsk) {
            assertThat(it.succeeded()).isFalse()
            assertThat(it.cause().message).isEqualTo("Login failed!")
            context.completeNow()
        }
    }

    @Test
    @Timeout(value = 5, timeUnit = SECONDS)
    internal fun do_login(vertx: Vertx, context: VertxTestContext) {
        val slsk = Slsk("username", "password", "localhost", 4321)
        vertx.deployVerticle(slsk) {
            assertThat(it.succeeded()).isTrue()
            context.completeNow()
        }
    }

}