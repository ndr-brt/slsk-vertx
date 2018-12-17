package ndr.brt.slsk

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.net.NetServer
import io.vertx.core.net.NetSocket
import org.slf4j.LoggerFactory

class MockServer(private val port: Int) : AbstractVerticle() {

    val log = LoggerFactory.getLogger(javaClass)

    override fun start(startFuture: Future<Void>) {
        val future = Future.future<NetServer>()
        vertx.createNetServer()
                .connectHandler(ConnectionHandler())
                .listen(port, future::handle)

        future.setHandler {
            log.info("Mock server started on port $port")
            startFuture.complete()
        }
    }

    private class ConnectionHandler: Handler<NetSocket> {
        override fun handle(socket: NetSocket) {
            socket.handler(BufferHandler(socket))
        }
    }

    private class BufferHandler(private val socket: NetSocket): Handler<Buffer> {
        override fun handle(buffer: Buffer) {

            val usernameLength = buffer.getIntLE(8)
            val username = buffer.getString(12, 12 + usernameLength)
            val passwordLength = buffer.getIntLE(12 + usernameLength)
            val password = buffer.getString(12 + usernameLength + 4, 12 + usernameLength + 4 + passwordLength)

            val message = when {
                password.contains("wrong") -> Protocol.FromServer.Login(false, "Login failed!")
                else -> Protocol.FromServer.Login(true, "Welcome to soulseek!")
            }
            socket.write(message.toChannel())
        }

    }
}