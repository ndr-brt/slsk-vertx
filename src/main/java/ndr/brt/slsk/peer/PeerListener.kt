package ndr.brt.slsk.peer

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.EventBus
import io.vertx.core.net.NetClient
import io.vertx.core.net.NetSocket
import ndr.brt.slsk.*
import ndr.brt.slsk.protocol.InputMessageHandler
import ndr.brt.slsk.protocol.Protocol
import ndr.brt.slsk.protocol.ProtocolBuffer
import org.slf4j.LoggerFactory

class PeerListener(address: Address, private val info: PeerInfo, peer: NetClient, private val eventBus: EventBus, callback: (AsyncResult<PeerListener>) -> Unit) {

    private val log = LoggerFactory.getLogger(javaClass)

    init {
        log.info("Starting Peer Listener")
        peer.connect(address.port, address.host) {
            if (it.succeeded()) {
                log.info("Connected with peer $address")
                PeerSocketHandler(info, eventBus).handle(it.result())
                it.result().write(Protocol.ToPeer.PierceFirewall(info.token).toChannel())
                callback(Future.succeededFuture(this))
            } else {
                log.error("Connection with peer failed", it.cause())
                callback(Future.failedFuture(it.cause()))
            }
        }
    }

    class PeerSocketHandler(private val info: PeerInfo, private val eventBus: EventBus): Handler<NetSocket> {

        private val log = LoggerFactory.getLogger(javaClass)

        override fun handle(socket: NetSocket) {
            socket.handler(InputMessageHandler("PeerInputMessage-${info.username}", eventBus))
            eventBus.consumer<Buffer>("PeerInputMessage-${info.username}") { message ->
                val inputMessage = ProtocolBuffer(message.body())
                log.info("Received message from user ${info.username} code ${inputMessage.code()}")
                when (inputMessage.code()) {
                    9 -> {
                        val unzip = inputMessage.decompress()
                        val username = unzip.readString()
                        val token = unzip.readToken()
                        val resultsCount = unzip.readInt()
                        val fileResult = mutableListOf<String>()
                        for (i in 0 until resultsCount) {
                            unzip.readByte()
                            val filename = unzip.readString()
                            val size1 = unzip.readInt()
                            val size2 = unzip.readInt()
                            unzip.readString()
                            val attributesCount = unzip.readInt()
                            for (j in 0 until attributesCount) {
                                unzip.readInt()
                                unzip.readInt()
                            }
                            fileResult.add(filename)
                        }
                        unzip.readByte()
                        unzip.readInt()
                        unzip.readInt()
                        log.info("Recv FileSearchResult da $username")
                        if (fileResult.size > 0) {
                            eventBus.emit(SearchResponded(fileResult))
                        }
                    }
                    else -> log.warn("Peer message unknown: ${inputMessage.code()}")
                }
            }
        }

    }

}
