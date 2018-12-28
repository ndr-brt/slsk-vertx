package ndr.brt.slsk

import io.vertx.core.json.JsonObject

interface Event {
    fun asJson(): JsonObject = JsonObject.mapFrom(this)
}

data class LoginRequested(val username: String = "", val password: String = ""): Event
data class LoginResponded(val succeed: Boolean = false, val message: String = ""): Event

data class SearchRequested(val query: String = "", val token: String = "00000000"): Event
data class SearchResponded(val files: List<String> = emptyList()): Event

data class ConnectToPeer(val username: String = "", val type: String = "", val host: String = "", val port: Int = 0, val token: String = ""): Event