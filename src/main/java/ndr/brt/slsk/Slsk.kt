package ndr.brt.slsk

import bytesToHex
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.core.json.JsonObject
import io.vertx.core.json.jackson.DatabindCodec
import ndr.brt.slsk.model.SearchResult
import ndr.brt.slsk.peer.PeerListener
import ndr.brt.slsk.peer.SharedFile
import ndr.brt.slsk.server.ServerListener
import org.slf4j.LoggerFactory
import java.net.SocketException
import java.nio.channels.ClosedChannelException
import kotlin.random.Random.Default.nextBytes
import kotlin.reflect.KClass

fun main() {
  val slsk = Slsk("ginogino", "ginogino", "server.slsknet.org", 2242)
  val vertx = Vertx.vertx()

  vertx.deployVerticle(slsk)
    .compose { slsk.search("leatherface", 3000) }
    .onSuccess { event ->
      event.filesWithSlot()
        .firstOrNull()
        .let { file ->
          if (file != null) {
            slsk.download(file)
          } else {
            println("No results found")
          }
        }
    }
    .onFailure { cause ->
      cause.printStackTrace()
    }
}

class Slsk(
  private val username: String,
  private val password: String,
  private val serverHost: String,
  private val serverPort: Int
) : AbstractVerticle() {

  private val log = LoggerFactory.getLogger(javaClass)
  private val searchResults: MutableMap<String, MutableList<SearchResponded>> = mutableMapOf()
  private val peers: MutableMap<String, PeerListener> = mutableMapOf()

  override fun start(start: Promise<Void>) {
    DatabindCodec.mapper().registerModule(KotlinModule())

    val serverListener = ServerListener(serverHost, serverPort, vertx.createNetClient(), vertx.eventBus())
    serverListener.connect()
      .compose { server -> server.login(username, password) }
      .onSuccess { login ->
        if (login.succeed) {
          log.info("Login succedeed: ${login.message}")
          start.complete()
        } else {
          log.error("Login failed: ${login.message}")
          start.fail(login.message)
        }
      }
      .onFailure { cause -> start.fail(cause) }

    vertx.eventBus().on(ConnectToPeer::class) { event ->
      val listener = PeerListener(event.address, event.info, vertx.createNetClient(), vertx.eventBus())
      listener.connect()
        .onSuccess {
          log.info("Connected to ${event.info.username} on ${event.address}")
          peers[event.info.username] = listener
        }
        .onFailure { cause ->
          when (cause) {
            is SocketException -> log.error("Error connecting to ${event.info.username} on ${event.address}: ${cause.localizedMessage}")
            is ClosedChannelException -> log.error("Closing connection to ${event.info.username} on ${event.address}")
            else -> log.error("Error connecting to ${event.info.username} on ${event.address}.", cause)
          }
        }
    }
  }

  fun search(query: String, timeout: Long): Future<SearchResult> {
    val token = nextBytes(4).let(bytesToHex)
    log.info("Search request $query with timeout $timeout and token $token")
    vertx.eventBus().emit(SearchRequested(query, token))

    searchResults[token] = mutableListOf()
    vertx.eventBus().on(SearchResponded::class) { event ->
      searchResults[token]!!.add(event)
    }

    val promise = Promise.promise<SearchResult>()
    vertx.setTimer(timeout) {
      promise.complete(SearchResult(token, searchResults[token].orEmpty()))
    }
    return promise.future()
  }

  fun download(file: SharedFile): Future<Unit> {
    val token = nextBytes(4).let(bytesToHex)
    val promise = Promise.promise<Unit>()

    val consumer = vertx.eventBus().on(DownloadNotAllowed::class) { event ->
      if (token == event.token) {
        if (event.reason.equals("Queued", true)) {
          // TODO: chessifa? wait for a message about?
        } else {
          promise.fail(event.reason)
        }
      }
    }

    peers[file.username]!!.transferRequest(token, file.filename)
    return promise.future()
      .onComplete { consumer.unregister() }
  }

}

fun EventBus.emit(event: Event): EventBus {
  return publish(event::class.java.simpleName, JsonObject.mapFrom(event))
}

fun <T: Event> EventBus.on(clazz: KClass<T>, function: (T) -> Unit): MessageConsumer<*> {
  return consumer<JsonObject>(clazz::java.get().simpleName) { message ->
    function.invoke(message.body().mapTo(clazz::java.get()))
  }
}
