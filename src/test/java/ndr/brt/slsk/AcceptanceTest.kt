package ndr.brt.slsk

import io.vertx.core.Vertx
import io.vertx.junit5.Timeout
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@ExtendWith(VertxExtension::class)
internal class AcceptanceTest {

    private val slsk = Slsk("ginogino", "ginogino", "server.slsknet.org", 2242)

    @BeforeEach
    internal fun setUp(vertx: Vertx) {
        val future = CompletableFuture<Void>()
        vertx.deployVerticle(slsk) {
            if (it.failed()) throw it.cause()
            future.complete(null)
        }
        future.get()
    }

    @Test
    @Timeout(value = 5, timeUnit = TimeUnit.SECONDS)
    internal fun search(context: VertxTestContext) {
        slsk.search("leatherface", 2000) {
            assertThat(it.files).isNotEmpty
            context.completeNow()
        }
    }
}