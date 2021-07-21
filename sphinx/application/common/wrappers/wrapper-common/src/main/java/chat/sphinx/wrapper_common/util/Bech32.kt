package chat.sphinx.wrapper_common.util

typealias Int5 = Byte

@Suppress("NOTHING_TO_INLINE")
inline fun String.isValidBech32(): Boolean =
    try {
        Bech32.decode(this)
        true
    } catch (e: Exception) {
        false
    }

/**
 * Class to handling Bech32 and Bech32m address formats.
 *
 * References:
 * - https://github.com/bitcoin/bips/blob/master/bip-0173.mediawiki
 * - https://github.com/bitcoin/bips/blob/master/bip-0350.mediawiki
 * - https://github.com/ACINQ/bitcoin-kmp/blob/9dcf89d04aa9b66bb6c8e446665bd529345fdfa1/src/commonMain/kotlin/fr/acinq/bitcoin/Bech32.kt
 */
class Bech32(
    val humanReadablePart: String,
    val data: Array<Int5>,
    val checksum: String,
    val encoding: Encoding
) {
    enum class Encoding(val constant: Int) {
        Bech32(1),
        Bech32m(0x2bc830a3)
    }

    companion object {
        private const val ALPHABET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"

        private const val CHECKSUM_LENGTH = 6

        private val byteToInt5Map = ALPHABET.mapIndexed { i, it ->
            it.code to i.toByte()
        }.toMap()

        fun decode(bolt11: String): Bech32 {
            if (bolt11.lowercase() != bolt11 && bolt11.uppercase() != bolt11) {
                throw Exception("Mixed case strings are not supported")
            }

            bolt11.lowercase().forEach {
                if (it.code !in 33..126) {
                    throw Exception("Invalid character in string")
                }
            }

            val input = bolt11.lowercase()
            val humanReadablePartEndPosition = input.lastIndexOf('1')
            val humanReadablePart = input.take(humanReadablePartEndPosition)

            if (humanReadablePart.length !in 1..83) {
                throw Exception("Human readable part needs to be of length 1 to 83")
            }

            val data = input.drop(humanReadablePartEndPosition+1).map { c ->
                byteToInt5Map[c.code]!!
            }



            return Bech32(
                humanReadablePart,
                data.dropLast(CHECKSUM_LENGTH).toTypedArray(),
                input.takeLast(CHECKSUM_LENGTH),
                verifyChecksum(humanReadablePart, data.toTypedArray())
            )
        }

        private fun verifyChecksum(humanReadablePart: String, data: Array<Int5>): Encoding {
            val values = expand(humanReadablePart) + data

            return when (polymod(values)) {
                Encoding.Bech32.constant -> Encoding.Bech32
                Encoding.Bech32m.constant -> Encoding.Bech32m
                else -> throw Exception("invalid checksum")
            }
        }

        /**
         * Expand human readable part of [Bech32] string into an array of [Int5]
         */
        private fun expand(humanReadablePart: String) : Array<Int5> {
            val result = ArrayList<Int5>()
            humanReadablePart.forEach { c ->
                result.add(c.code.shr(5).toByte())
            }
            result.add(0)
            humanReadablePart.forEach { c ->
                result.add((c.code and 31).toByte())
            }
            return result.toTypedArray()
        }

        private fun polymod(values: Array<Int5>): Int {
            val GEN = arrayOf(0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3)
            var chk = 1
            values.forEach { v ->
                val b = chk shr 25
                chk = ((chk and 0x1ffffff) shl 5) xor v.toInt()
                for (i in 0..5) {
                    if (((b shr i) and 1) != 0) chk = chk xor GEN[i]
                }
            }
            return chk
        }

        /**
         * @param input a sequence of 8 bits integers
         * @return a sequence of 5 bits integers
         */
        fun eight2five(input: ByteArray): Array<Int5> {
            var buffer = 0L
            val output = ArrayList<Int5>()
            var count = 0
            input.forEach { b ->
                buffer = (buffer shl 8) or (b.toLong() and 0xff)
                count += 8
                while (count >= 5) {
                    output.add(((buffer shr (count - 5)) and 31).toByte())
                    count -= 5
                }
            }
            if (count > 0) output.add(((buffer shl (5 - count)) and 31).toByte())
            return output.toTypedArray()
        }

        /**
         * @param input a sequence of 5 bits integers
         * @return a sequence of 8 bits integers
         */
        fun five2eight(input: Array<Int5>, offset: Int): ByteArray {
            var buffer = 0L
            val output = ArrayList<Byte>()
            var count = 0
            for (i in offset..input.lastIndex) {
                val b = input[i]
                buffer = (buffer shl 5) or (b.toLong() and 31)
                count += 5
                while (count >= 8) {
                    output.add(((buffer shr (count - 8)) and 0xff).toByte())
                    count -= 8
                }
            }
            require(count <= 4) { "Zero-padding of more than 4 bits" }
            require((buffer and ((1L shl count) - 1L)) == 0L) { "Non-zero padding in 8-to-5 conversion" }
            return output.toByteArray()
        }

        fun u32(input: Array<Int5>): Long {
            var result = 0L
            input.forEach { result = result * 32 + it }
            return result
        }
    }
}