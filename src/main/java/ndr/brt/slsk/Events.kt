package ndr.brt.slsk

import io.vertx.core.json.JsonObject

interface Event {
    fun asJson(): JsonObject = JsonObject.mapFrom(this)
}

data class LoginRequested(val username: String = "", val password: String = ""): Event
data class LoginResponded(val succeed: Boolean = false, val message: String = ""): Event