package ndr.brt.slsk

interface Event

data class LoginRequested(val username: String = "", val password: String = ""): Event
data class LoginResponded(val succeed: Boolean = false, val message: String = ""): Event

data class SearchRequested(val query: String = "", val token: String = "00000000"): Event
data class SearchResponded(val files: List<String> = emptyList()): Event

data class ConnectToPeer(val address: Address = Address(), val info: PeerInfo = PeerInfo()): Event