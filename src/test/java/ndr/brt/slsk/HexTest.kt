package ndr.brt.slsk

import bytesToHex
import hexToBytes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class HexTest {
    @Test
    internal fun `bytes to hex`() {
        val hex = bytesToHex(byteArrayOf(-85, -51, -17, 1))

        assertThat(hex).isEqualTo("abcdef01")
    }

    @Test
    internal fun `hex to byte`() {
        val bytes = hexToBytes("abcdef01")

        assertThat(bytes).isEqualTo(byteArrayOf(-85, -51, -17, 1))
    }
}