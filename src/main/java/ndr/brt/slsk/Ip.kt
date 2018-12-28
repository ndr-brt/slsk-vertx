package ndr.brt.slsk

data class Ip(val pos3: Short, val pos2: Short, val pos1: Short, val pos0: Short) {
    override fun toString(): String {
        return "$pos0.$pos1.$pos2.$pos3"
    }
}
