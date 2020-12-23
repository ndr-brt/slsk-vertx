package ndr.brt.slsk

import io.vertx.core.Vertx
import io.vertx.junit5.Timeout
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.TimeUnit

@ExtendWith(VertxExtension::class)
internal class AcceptanceTest {

  private val slsk = Slsk("ginogino", "ginogino", "server.slsknet.org", 2242)

  @BeforeEach
  internal fun setUp(vertx: Vertx, setup: VertxTestContext) {
    vertx.deployVerticle(slsk, setup.succeedingThenComplete())
  }

  @Test
  @Timeout(value = 5, timeUnit = TimeUnit.SECONDS)
  internal fun search(test: VertxTestContext) {
    slsk.search("leatherface", 2000)
      .onSuccess { result ->
        assertThat(result.files()).isNotEmpty
        test.completeNow()
      }
      .onFailure(test::failNow)
  }
//
//  @Test
//  @Timeout(value = 60, timeUnit = TimeUnit.SECONDS)
//  internal fun download(test: VertxTestContext) {
//    slsk.search("leatherface", 2000)
//      .map { aggregated -> aggregated.filesWithSlot() }
//      .compose { files -> slsk.download(files[0]) }
//      .onSuccess {
//        test.completeNow()
//      }
//      .onFailure(test::failNow)
//  }

}