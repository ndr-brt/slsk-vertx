package ndr.brt.slsk

data class Address(val host: String = "", val port: Int = 0) {
    override fun toString(): String = "$host:$port"
}