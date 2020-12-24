package ndr.brt.slsk.peer

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
import ndr.brt.slsk.protocol.TransferDirection
import ndr.brt.slsk.protocol.TransferDirection.Download
import ndr.brt.slsk.protocol.TransferDirection.Upload
import org.slf4j.LoggerFactory

class PeerListener(
  private val address: Address,
  private val info: PeerInfo,
  private val peer: NetClient,
  private val eventBus: EventBus
) {

  private val log = LoggerFactory.getLogger(javaClass)

  fun connect(): Future<Unit> {
    return peer.connect(address.port, address.host)
      .compose { result ->
        PeerSocketHandler(info, eventBus).handle(result)
        result.write(Protocol.ToPeer.PierceFirewall(info.token).toChannel())
      }
      .map(Unit)
  }

  fun transferRequest(token: String, filename: String) {
    log.info("send TransferRequest to ${info.username} for $filename")
    eventBus.emit(TransferRequested(info.username, token, filename))
  }

  class PeerSocketHandler(private val info: PeerInfo, private val eventBus: EventBus) : Handler<NetSocket> {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(socket: NetSocket) {
      socket.handler(InputMessageHandler("PeerInputMessage-${info.username}", eventBus))
      eventBus.consumer<Buffer>("PeerInputMessage-${info.username}") { message ->
        val inputMessage = ProtocolBuffer(message.body())
        when (inputMessage.code()) {
          4 -> sharesRequest
          9 -> fileSearchResult
          40 -> transferRequest
          41 -> transferResponse
          46 -> uploadFailed
          else -> unknownMessage
        }.invoke(inputMessage)
      }

      eventBus.on(TransferRequested::class) { event ->
        if (event.username == info.username) {
          socket.write(Protocol.ToPeer.TransferRequest(event.token, event.filename).toChannel())
        }
      }

      eventBus.on(UploadRequested::class) { event ->
        if (event.username == info.username) {
          log.info("send TransferResponse to ${info.username}")
          socket.write(Protocol.ToPeer.TransferResponse(event.token).toChannel())
        }
      }

      eventBus.on(DownloadRequested::class) { event ->
        if (event.username == info.username) {
          log.warn("download requested by ${info.username}. NOT YET IMPLEMENTED")
          socket.write(Protocol.ToPeer.TransferResponse(event.token).toChannel())
        }
      }
    }

    private val uploadFailed: (ProtocolBuffer) -> Unit = {
      val filename = it.readString()
      log.info("${info.username}: UploadFailed for $filename")
    }

    private val sharesRequest: (ProtocolBuffer) -> Unit = {
      log.info("${info.username} requested our shares")
    }

    private val transferRequest: (ProtocolBuffer) -> Unit = {
      val direction = it.readTransferDirection()
      val token = it.readToken()
      val filename = it.readString()
      log.info("${info.username}: TransferRequest $direction for $filename")
      when (direction) {
        Upload -> eventBus.emit(UploadRequested(info.username, token, filename))
        Download -> eventBus.emit(DownloadRequested(info.username, token, filename))
      }
    }

    private val transferResponse: (ProtocolBuffer) -> Unit = {
      val token = it.readToken()
      val allowed = it.readBoolean()
      if (allowed) {
        log.info("${info.username}: Download allowed")
        eventBus.emit(DownloadAllowed(token))
      } else {
        val reason = it.readString()
        log.warn("${info.username}: Download not allowed because $reason")
        eventBus.emit(DownloadNotAllowed(token, reason))
      }
    }

    private val unknownMessage: (ProtocolBuffer) -> Unit = { inputMessage ->
      log.warn("Peer message unknown from ${info.username}: ${inputMessage.code()}")
    }

    private val fileSearchResult: (ProtocolBuffer) -> Unit = { inputMessage ->
      val unzip = inputMessage.decompress()
      val username = unzip.readString()
      val token = unzip.readToken()
      val resultsCount = unzip.readInt()
      val files = mutableListOf<SharedFile>()
      for (i in 0 until resultsCount) {
        unzip.readByte()
        val filename = unzip.readString()
        val size = unzip.readInt()
        unzip.readInt()
        unzip.readString()
        val attributesCount = unzip.readInt()
        for (j in 0 until attributesCount) {
          unzip.readInt()
          unzip.readInt()
        }
        files.add(SharedFile(username, filename, size))
      }
      val slots = unzip.readBoolean()
      val uploadSpeed = unzip.readInt()
      unzip.readInt()
      log.info("Recv FileSearchResult da $username")
      if (files.size > 0) {
        eventBus.emit(SearchResponded(token, files, slots, uploadSpeed))
      }
    }

  }

}
