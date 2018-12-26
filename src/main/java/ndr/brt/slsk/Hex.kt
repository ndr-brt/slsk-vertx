private const val hexArray = "0123456789abcdef"
private val hexChars = hexArray.toCharArray()

val bytesToHex: (ByteArray) -> String = { bytes ->
    val result = StringBuffer()

    bytes.forEach {
        val octet = it.toInt()
        val firstIndex = (octet and 0xF0).ushr(4)
        val secondIndex = octet and 0x0F
        result.append(hexChars[firstIndex])
        result.append(hexChars[secondIndex])
    }

    result.toString()
}

val hexToBytes: (String) -> ByteArray = { hex ->
    val result = ByteArray(hex.length / 2)

    for (i in 0 until hex.length step 2) {
        val firstIndex = hexArray.indexOf(hex[i])
        val secondIndex = hexArray.indexOf(hex[i + 1])

        val octet = firstIndex.shl(4).or(secondIndex)
        result[i.shr(1)] = octet.toByte()
    }

    result
}