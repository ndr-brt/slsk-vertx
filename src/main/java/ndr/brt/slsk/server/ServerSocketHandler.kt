package ndr.brt.slsk.server

import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.EventBus
import io.vertx.core.net.NetSocket
import ndr.brt.slsk.*
import ndr.brt.slsk.peer.PeerInfo
import ndr.brt.slsk.protocol.InputMessageHandler
import ndr.brt.slsk.protocol.Protocol
import ndr.brt.slsk.protocol.ProtocolBuffer
import org.slf4j.LoggerFactory

class ServerSocketHandler(private val eventBus: EventBus): Handler<NetSocket> {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(socket: NetSocket) {
        socket.handler(InputMessageHandler("ServerInputMessage", eventBus))

        eventBus.consumer<Buffer>("ServerInputMessage") { message ->
            ProtocolBuffer(message.body()).let {
                when (it.code()) {
                    1 -> login
                    18 -> connectToPeer
                    64 -> numberOfRooms
                    69 -> privilegedUsers
                    83 -> parentMinSpeed
                    84 -> parentSpeedRatio
                    104 -> wishListInterval
                    else -> unknownMessage
                }.invoke(it)
            }
        }

        eventBus.on(LoginRequested::class) { event ->
            val login = Protocol.ToServer.Login(event.username, event.password)
            log.info("Send login message to server")
            socket.write(login.toChannel())
        }

        eventBus.on(SearchRequested::class) { event ->
            val fileSearch = Protocol.ToServer.FileSearch(event.token, event.query)
            log.info("Send file search message to server")
            socket.write(fileSearch.toChannel())
        }
    }

    private val unknownMessage: (ProtocolBuffer) -> Unit = { inputMessage ->
        log.warn("Server message code ${inputMessage.code()} unknown")
    }

    private val wishListInterval: (ProtocolBuffer) -> Unit = { inputMessage ->
        val wishlistInterval = inputMessage.readInt()
        log.info("Recv WishlistInterval: $wishlistInterval")
    }

    private val parentSpeedRatio: (ProtocolBuffer) -> Unit = { inputMessage ->
        val parentSpeedRatio = inputMessage.readInt()
        log.info("Recv ParentSpeedRatio: $parentSpeedRatio")
    }

    private val parentMinSpeed: (ProtocolBuffer) -> Unit = { inputMessage ->
        val parentMinSpeed = inputMessage.readInt()
        log.info("Recv ParentMinSpeed: $parentMinSpeed")
    }

    private val privilegedUsers: (ProtocolBuffer) -> Unit = { inputMessage ->
        val privilegedUsers = inputMessage.readInt()
        log.info("Recv PrivilegedUsers: $privilegedUsers")
    }

    private val numberOfRooms: (ProtocolBuffer) -> Unit = { inputMessage ->
        val numberOfRooms = inputMessage.readInt()
        log.info("Recv RoomList: $numberOfRooms")
    }

    private val connectToPeer: (ProtocolBuffer) -> Unit = { inputMessage ->
        val username = inputMessage.readString()
        val type = inputMessage.readString()
        val ip = inputMessage.readIp()
        val port = inputMessage.readInt()
        val token = inputMessage.readToken()

        val address = Address(ip.toString(), port)
        val info = PeerInfo(username, type, token)
        log.info("Recv ConnectToPeer: $address, $info")
        eventBus.emit(ConnectToPeer(address, info))
    }

    private val login: (ProtocolBuffer) -> Unit = { inputMessage ->
        log.info("Recv Login")
        eventBus.emit(LoginResponded(inputMessage.readByte().toInt() == 1, inputMessage.readString()))
    }

}
