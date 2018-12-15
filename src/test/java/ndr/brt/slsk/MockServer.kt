package ndr.brt.slsk

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.net.NetServer
import io.vertx.core.net.NetSocket

class MockServer(private val port: Int) : AbstractVerticle() {
    override fun start(startFuture: Future<Void>) {
        val future = Future.future<NetServer>()
        vertx.createNetServer()
                .connectHandler(ConnectionHandler())
                .listen(port, future::handle)

        future.setHandler {
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
            val message = when {
                buffer.toString().contains("username") -> Protocol.FromServer.Login(true, "Welcome to soulseek!")
                else -> Protocol.FromServer.Login(false, "Login failed!")
            }
            val messageBuffer = message.toBuffer()
            socket.write(Buffer.buffer()
                    .appendIntLE(messageBuffer.length())
                    .appendBuffer(messageBuffer))
        }

    }
}