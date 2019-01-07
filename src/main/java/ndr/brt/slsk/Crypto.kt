package ndr.brt.slsk

import java.security.MessageDigest

val md5: (String) -> ByteArray = { string ->
    MessageDigest.getInstance("MD5").digest(string.toByteArray())
}