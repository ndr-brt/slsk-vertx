package ndr.brt.slsk

import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.buffer.Buffer.buffer
import io.vertx.junit5.Timeout
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import ndr.brt.slsk.protocol.InputMessageHandler
import ndr.brt.slsk.protocol.appendStringWithLength
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.TimeUnit

@ExtendWith(VertxExtension::class)
class InputMessageHandlerTest {

  @Test
  @Timeout(value = 1, timeUnit = TimeUnit.SECONDS)
  internal fun handle_small_message(vertx: Vertx, context: VertxTestContext) {
    val handler = InputMessageHandler("publish-address", vertx.eventBus())
    val message = buffer()
      .appendIntLE(17)
      .appendIntLE(1)
      .appendByte(1)
      .appendStringWithLength("12345678")

    vertx.eventBus().consumer<Buffer>("publish-address") {
      assert(it.body() == message)
      context.completeNow()
    }

    handler.handle(message)
  }

  @Test
  @Timeout(value = 1, timeUnit = TimeUnit.SECONDS)
  internal fun handle_message_splitted_in_two_parts(vertx: Vertx, context: VertxTestContext) {
    val handler = InputMessageHandler("publish-address", vertx.eventBus())
    val partOne = buffer()
      .appendIntLE(17)
      .appendIntLE(1)
      .appendByte(1)

    val partTwo = buffer()
      .appendStringWithLength("12345678")

    vertx.eventBus().consumer<Buffer>("publish-address") {
      assert(it.body() == partOne.appendBuffer(partTwo))
      context.completeNow()
    }

    handler.handle(partOne)
    handler.handle(partTwo)
  }

  @Test
  @Timeout(value = 1, timeUnit = TimeUnit.SECONDS)
  internal fun handle_two_messages_in_one_packet(vertx: Vertx, context: VertxTestContext) {
    val handler = InputMessageHandler("publish-address", vertx.eventBus())
    val message = buffer()
      .appendIntLE(17)
      .appendIntLE(1)
      .appendByte(1)
      .appendStringWithLength("12345678")

    vertx.eventBus().consumer<Buffer>("publish-address") {
      assert(it.body() == message)
      context.completeNow()
    }

    handler.handle(buffer().appendBuffer(message).appendBuffer(message))
  }
}