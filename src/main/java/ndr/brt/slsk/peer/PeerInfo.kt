package ndr.brt.slsk.peer

data class PeerInfo(val username: String = "", val type: String = "", val token: String = "") {
    override fun toString(): String = "user: $username, type: $type, token: $token"
}
