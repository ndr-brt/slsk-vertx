package ndr.brt.slsk.server

import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.eventbus.EventBus
import io.vertx.core.net.NetClient
import ndr.brt.slsk.LoginRequested
import ndr.brt.slsk.LoginResponded
import ndr.brt.slsk.emit
import ndr.brt.slsk.on
import org.slf4j.LoggerFactory

class ServerListener(
  private val serverHost: String,
  private val serverPort: Int,
  private val server: NetClient,
  private val eventBus: EventBus
) {

  private val log = LoggerFactory.getLogger(javaClass)

  fun connect(): Future<ServerListener> {
    log.info("Starting Server Listener")
    return server.connect(serverPort, serverHost)
      .map { socket ->
        log.info("Connected with server {}:{}", serverHost, serverPort)
        ServerSocketHandler(eventBus).handle(socket)
      }
      .map(this)
  }

  fun login(username: String, password: String): Future<LoginResponded> {
    log.info("Login with username {}", username)
    val promise = Promise.promise<LoginResponded>()
    eventBus
      .on(LoginResponded::class) { loginResponded -> promise.complete(loginResponded) }
      .emit(LoginRequested(username, password))
    return promise.future()
  }

}