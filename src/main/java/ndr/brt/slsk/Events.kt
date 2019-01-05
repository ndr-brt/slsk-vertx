package ndr.brt.slsk

import ndr.brt.slsk.peer.PeerInfo
import ndr.brt.slsk.peer.SharedFile

interface Event

data class LoginRequested(val username: String = "", val password: String = ""): Event
data class LoginResponded(val succeed: Boolean = false, val message: String = ""): Event

data class SearchRequested(val query: String = "", val token: String = "00000000"): Event
data class SearchResponded(val token: String = "", val files: List<SharedFile> = emptyList(), val slots: Boolean = false): Event
data class SearchResultsAggregated(val token: String = "", val results: List<SearchResponded> = emptyList()): Event

data class ConnectToPeer(val address: Address = Address(), val info: PeerInfo = PeerInfo()): Event

data class TransferRequested(val username: String = "", val token: String = "", val filename: String = ""): Event
data class TransferResponded(val username: String = "", val token: String = ""): Event