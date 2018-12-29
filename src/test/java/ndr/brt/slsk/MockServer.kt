package ndr.brt.slsk

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.net.NetServer
import io.vertx.core.net.NetSocket
import ndr.brt.slsk.protocol.Protocol
import ndr.brt.slsk.protocol.parseToServerMessage
import org.slf4j.LoggerFactory

class MockServer(private val port: Int) : AbstractVerticle() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun start(startFuture: Future<Void>) {
        val future = Future.future<NetServer>()
        vertx.createNetServer()
                .connectHandler { socket -> socket.handler(BufferHandler(socket))}
                .listen(port, future::handle)

        future.setHandler {
            if (it.failed()) startFuture.fail(it.cause())

            log.info("Mock server started on port $port")
            startFuture.complete()
        }
    }

    private class BufferHandler(private val socket: NetSocket): Handler<Buffer> {

        private val log = LoggerFactory.getLogger(javaClass)

        override fun handle(buffer: Buffer) {

            val message = parseToServerMessage(buffer)
            when (message) {
                is Protocol.ToServer.Login -> {
                    socket.write(when {
                        message.password.contains("wrong") -> Protocol.FromServer.Login(false, "Login failed!")
                        else -> Protocol.FromServer.Login(true, "Welcome to soulseek!")
                    }.toChannel())
                }
                else -> log.info("Unknown message type ${message.type()}")
            }
        }
    }
}