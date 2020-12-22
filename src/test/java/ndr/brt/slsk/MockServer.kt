package ndr.brt.slsk

import io.vertx.core.AbstractVerticle
import io.vertx.core.Handler
import io.vertx.core.Promise
import io.vertx.core.buffer.Buffer
import io.vertx.core.net.NetServer
import io.vertx.core.net.NetSocket
import ndr.brt.slsk.protocol.Protocol
import ndr.brt.slsk.protocol.ProtocolBuffer
import org.slf4j.LoggerFactory

class MockServer(private val port: Int) : AbstractVerticle() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun start(start: Promise<Void>) {
        val promise = Promise.promise<NetServer>()
        vertx.createNetServer()
                .connectHandler { socket -> socket.handler(BufferHandler(socket))}
                .listen(port, promise)

        promise.future().onComplete {
            if (it.failed()) start.fail(it.cause())

            log.info("Mock server started on port $port")
            start.complete()
        }
    }

    private class BufferHandler(private val socket: NetSocket): Handler<Buffer> {

        private val log = LoggerFactory.getLogger(javaClass)

        override fun handle(input: Buffer) {
            val buffer = ProtocolBuffer(input)
          when (val type = buffer.code()) {
                1 -> {
                    val message = Protocol.ToServer.Login(buffer.readString(), buffer.readString())
                    socket.write(when {
                        message.password.contains("wrong") -> Protocol.FromServer.Login(false, "Login failed!")
                        else -> Protocol.FromServer.Login(true, "Welcome to soulseek!")
                    }.toChannel())
                }
                else -> log.info("Unknown message type $type")
            }

        }
    }
}