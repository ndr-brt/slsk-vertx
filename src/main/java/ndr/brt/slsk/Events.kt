package ndr.brt.slsk

interface Event

data class Login(val succeed: Boolean = false, val message: String = ""): Event