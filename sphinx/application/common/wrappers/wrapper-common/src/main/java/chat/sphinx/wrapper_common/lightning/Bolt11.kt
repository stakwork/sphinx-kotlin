package chat.sphinx.wrapper_common.lightning

import chat.sphinx.wrapper_common.util.Bech32
import chat.sphinx.wrapper_common.util.Int5
import io.matthewnelson.crypto_common.clazzes.toSha256Hash
import io.matthewnelson.crypto_common.extensions.toHex
import java.io.ByteArrayInputStream
import kotlin.jvm.Throws

/**
 * Class used to handling bolt11 payment requests
 *
 * References:
 * - https://github.com/lightningnetwork/lightning-rfc/blob/84213f45c05a39dd0812604a6a11d9d3c1207425/11-payment-encoding.md
 * - https://github.com/stakwork/sphinx-ios/blob/1b1670113c2ac6fecee9d10917fdb1d7f57e8966/sphinx/Helpers/PaymentRequestDecoder.swift
 * - https://github.com/ACINQ/lightning-kmp/blob/ce7120f668be0aa057020a3bb6c93eab5d6ad0e9/src/commonMain/kotlin/fr/acinq/lightning/payment/PaymentRequest.kt
 *
 */
class Bolt11(
    val prefix: String,
    val amount: MilliSat?,
    val timestampSeconds: Long,
    val tags: Array<TaggedField>,
    val checksum: String,
    val signature: String,
    val recoveryFlag: Byte,
    val nodeId: String
) {
    companion object {
        private val PREFIXES = arrayOf(
            "lnbcrt",
            "lntb",
            "lnbc"
        )

        private const val SIGNATURE_LENGTH = 104
        private const val TIMESTAMP_LENGTH = 7

        @Throws(Exception::class)
        fun decode(lightningPaymentRequest: LightningPaymentRequest): Bolt11 {
            val paymentRequest = lightningPaymentRequest.value.replaceFirst("lightning:", "")

            if (!paymentRequest.lowercase().startsWith("ln")) {
                throw Exception("Not a valid Lightning Payment Request")
            }
            val bech32PaymentRequest = Bech32.decode(paymentRequest.lowercase())

            val prefix = PREFIXES.find {
                bech32PaymentRequest.humanReadablePart.startsWith(it)
            } ?: throw Exception("unknown prefix ${bech32PaymentRequest.humanReadablePart}")
            val amount = decodeAmount(bech32PaymentRequest.humanReadablePart.drop(prefix.length))
            val timestamp = decodeTimestamp(bech32PaymentRequest.data)
            val taggedFields = TaggedField.deduceTaggedFields(
                bech32PaymentRequest.data.drop(TIMESTAMP_LENGTH).dropLast(SIGNATURE_LENGTH)
            )

            // TODO: Verify signature and deduce the nodeId...
            val signatureByteArray = Bech32.five2eight(
                bech32PaymentRequest.data.takeLast(SIGNATURE_LENGTH).toTypedArray(),
                0
            )
            val recoveryFlag = signatureByteArray.last()
            require(recoveryFlag in arrayOf(0.toByte(),1.toByte(),2.toByte(),3.toByte())) {
                "Invalid recovery flag"
            }
            require(signatureByteArray.size == 65) {
                "Signature not of valid length"
            }
            val data = bech32PaymentRequest.humanReadablePart.encodeToByteArray() + bech32PaymentRequest.data.dropLast(
                SIGNATURE_LENGTH).toByteArray()
            val signature = signatureByteArray.dropLast(1).toByteArray()
            val message = data.toSha256Hash()
            val nodeId = recoverPublicKey(signature, message.value.toByteArray(), recoveryFlag)
            // TODO: Verify Signature using Secp256k1

            return Bolt11(
                prefix,
                amount,
                timestamp,
                taggedFields,
                bech32PaymentRequest.checksum,
                signature.toHex(),
                recoveryFlag,
                nodeId.toHex()
            )
        }

        private fun recoverPublicKey(signature: ByteArray, message: ByteArray, recoveryFlag: Byte): ByteArray {
            // TODO: Recover public key/nodeId using Secp256k1
            return byteArrayOf()
        }

        private fun decodeAmount(input: String): MilliSat? {
            val amount = when {
                input.isEmpty() -> null
                input.last() == 'p' -> MilliSat(input.dropLast(1).toLong() / 10L)
                input.last() == 'n' -> MilliSat(input.dropLast(1).toLong() * 100L)
                input.last() == 'u' -> MilliSat(input.dropLast(1).toLong() * 100000L)
                input.last() == 'm' -> MilliSat(input.dropLast(1).toLong() * 100000000L)
                else -> MilliSat(input.toLong() * 100000000000L)
            }
            return if (amount == MilliSat(0)) null else amount
        }

        private fun decodeTimestamp(input: Array<Int5>): Long = input.take(TIMESTAMP_LENGTH).fold(0L) { a, b -> 32 * a + b }
    }

    sealed class TaggedField {
        abstract val tag: Int5

        companion object {
            fun deduceTaggedFields(input: List<Int5>): Array<TaggedField> {
                if (input.isNotEmpty()) {
                    val tag = input.first()
                    val length = 32 * input[1] + input [2]
                    val value = input.drop(3).take(length)
                    return arrayOf(
                        kotlin.runCatching {
                            when (tag) {
                                PaymentHash.tag -> PaymentHash.decode(value)
                                PaymentSecret.tag -> PaymentSecret.decode(value)
                                Description.tag -> Description.decode(value)
                                DescriptionHash.tag -> DescriptionHash.decode(value)
                                Expiry.tag -> Expiry.decode(value)
                                MinFinalCltvExpiry.tag -> MinFinalCltvExpiry.decode(value)
                                FallbackAddress.tag -> FallbackAddress.decode(value)
                                Features.tag -> Features.decode(value)
                                RoutingInfo.tag -> RoutingInfo.decode(value)
                                else -> {
                                    // We got a tag which we do not have logic to decode
                                    UnknownTag(tag, value)
                                }
                            }
                        }.getOrDefault(InvalidTag(tag, value))
                    ) + deduceTaggedFields(input.drop(3+length))
                }
                return arrayOf()
            }
        }

        /**
         * Tag `d` (13) of variable length which is a short description of purpose of payment (UTF-8)
         *
         * @param description a free-format string that will be included in the payment request
         */
        data class Description(val description: String) : TaggedField() {
            override val tag: Int5 = Description.tag
            fun encode(): List<Int5> = Bech32.eight2five(description.encodeToByteArray()).toList()

            companion object {
                const val tag: Int5 = 13
                fun decode(input: List<Int5>): Description = Description(Bech32.five2eight(input.toTypedArray(), 0).decodeToString())
            }
        }

        /**
         * Tag `h` (23) of length 52 which has the 256-bit description of purpose of payment (SHA256).
         *
         * This is used to commit to an associated description that is over 639 bytes,
         * but the transport mechanism for the description in that case is transport specific and not defined here.
         *
         * @param hash sha256 hash of an associated description
         */
        data class DescriptionHash(val hash: ByteArray) : TaggedField() {
            override val tag: Int5 = DescriptionHash.tag
            fun encode(): List<Int5> = Bech32.eight2five(hash).toList()

            companion object {
                const val tag: Int5 = 23
                private const val EXPECTED_LENGTH: Int = 52
                fun decode(input: List<Int5>): DescriptionHash {
                    require(input.size == EXPECTED_LENGTH)
                    return DescriptionHash(Bech32.five2eight(input.toTypedArray(), 0))
                }
            }
        }

        /**
         * Tag `p` (1) of length 52 used to store the payment hash
         * @param hash payment hash
         */
        data class PaymentHash(val hash: ByteArray) : TaggedField() {
            override val tag: Int5 = PaymentHash.tag
            fun encode(): List<Int5> = Bech32.eight2five(hash).toList()

            companion object {
                const val tag: Int5 = 1
                private const val EXPECTED_LENGTH: Int = 52
                fun decode(input: List<Int5>): PaymentHash {
                    require(input.size == EXPECTED_LENGTH)
                    return PaymentHash(Bech32.five2eight(input.toTypedArray(), 0))
                }
            }
        }

        /**
         * Tag `s` (16) of length 52 used to store the payment secret
         *
         * @param secret payment secret
         */
        data class PaymentSecret(val secret: ByteArray) : TaggedField() {
            override val tag: Int5 = PaymentSecret.tag
            fun encode(): List<Int5> = Bech32.eight2five(secret).toList()

            companion object {
                const val tag: Int5 = 16
                private const val EXPECTED_LENGTH: Int = 52
                fun decode(input: List<Int5>): PaymentSecret {
                    require(input.size == EXPECTED_LENGTH)
                    return PaymentSecret(Bech32.five2eight(input.toTypedArray(), 0))
                }
            }
        }

        /**
         * Tag 'x' (6) of variable length used to store when a transaction expires
         * @param expirySeconds payment expiry (in seconds)
         */
        data class Expiry(val expirySeconds: Long) : TaggedField() {
            override val tag: Int5 = Expiry.tag
            fun encode(): List<Int5> {
                tailrec fun loop(value: Long, acc: List<Int5>): List<Int5> = if (value == 0L) acc.reversed() else {
                    loop(value / 32, acc + (value.rem(32)).toByte())
                }
                return loop(expirySeconds, listOf())
            }

            companion object {
                const val tag: Int5 = 6
                fun decode(input: List<Int5>): Expiry {
                    return Expiry(Bech32.u32(input.toTypedArray()))
                }
            }
        }

        /**
         * Tag `c` (24) of variable length used to store min_final_cltv_expiry for the last HTLC in the route.
         *
         * Default is 18 if not specified.
         *
         * @param cltvExpiry minimum final expiry delta
         */
        data class MinFinalCltvExpiry(val cltvExpiry: Long) : TaggedField() {
            override val tag: Int5 = MinFinalCltvExpiry.tag
            fun encode(): List<Int5> {
                tailrec fun loop(value: Long, acc: List<Int5>): List<Int5> = if (value == 0L) acc.reversed() else {
                    loop(value / 32, acc + (value.rem(32)).toByte())
                }
                return loop(cltvExpiry, listOf())
            }

            companion object {
                const val tag: Int5 = 24
                fun decode(input: List<Int5>): MinFinalCltvExpiry {
                    return MinFinalCltvExpiry(Bech32.u32(input.toTypedArray()))
                }
            }
        }

        /**
         * Tag 'f' (9) of variable length Fallback on-chain payment address to be used if LN payment cannot be processed
         *
         * @param version minimum final expiry delta
         * @param data minimum final expiry delta
         */
        data class FallbackAddress(val value: List<Int5>) : TaggedField() {
            override val tag: Int5 = FallbackAddress.tag
            fun encode(): List<Int5> = value

            companion object {
                const val tag: Int5 = 9

                fun decode(input: List<Int5>): FallbackAddress = FallbackAddress(input)
            }
        }

        /**
         * Tag `9` (5) of variable length used to store supported features
         *
         * @param bits minimum final expiry delta
         */
        data class Features(val bits: ByteArray) : TaggedField() {
            override val tag: Int5 = Features.tag

            fun encode(): List<Int5> {
                // We pad left to a multiple of 5
                val padded = bits.toMutableList()
                while (padded.size * 8 % 5 != 0) {
                    padded.add(0, 0)
                }
                // Then we remove leading 0 bytes
                return Bech32.eight2five(padded.toByteArray()).dropWhile { it == 0.toByte() }
            }

            companion object {
                const val tag: Int5 = 5
                fun decode(input: List<Int5>): Features {
                    // We pad left to a multiple of 8
                    val padded = input.toMutableList()
                    while (padded.size * 5 % 8 != 0) {
                        padded.add(0, 0)
                    }
                    // Then we remove leading 0 bytes
                    val features = Bech32.five2eight(padded.toTypedArray(), 0).dropWhile { it == 0.toByte() }
                    return Features(features.toByteArray())
                }
            }
        }

        /**
         * Tag `r` (3) of variable length. One or more entries containing extra routing information for a private route
         */
        data class RoutingInfo(val hints: List<ExtraHop>) : TaggedField() {
            override val tag: Int5 = RoutingInfo.tag

            companion object {
                const val tag: Int5 = 3

                fun decode(input: List<Int5>): RoutingInfo {
                    val hints = ArrayList<ExtraHop>()
                    val stream = ByteArrayInputStream(Bech32.five2eight(input.toTypedArray(), 0))

                    while (stream.available() >= 51) {
                        hints.add(
                            ExtraHop(
                                LightningNodePubKey.fromByteArray(
                                    bytesToByteArray(stream, 33)
                                ),
                                byteArrayToHexString(
                                    bytesToByteArray(stream, 8)
                                ),
                                MilliSat(
                                    byteArrayToLongBE(
                                        bytesToByteArray(stream, 4)
                                    )
                                ),
                                byteArrayToLongBE(
                                    bytesToByteArray(stream, 4)
                                ),
                                byteArrayToIntBE(
                                    bytesToByteArray(stream, 2)
                                )
                            )
                        )
                    }

                    return RoutingInfo(hints)
                }

                private fun bytesToByteArray(input: ByteArrayInputStream, size: Int): ByteArray {
                    val result = ByteArray(size)
                    if (size > 0) {
                        val count = input.read(result, 0, result.size)
                        require(count >= size) { "unexpected EOF" }
                    }
                    return result
                }
                private fun byteArrayToHexString(byteArray: ByteArray): String {
                    return byteArray.toHex()
                }

                private fun byteArrayToIntBE(byteArray: ByteArray, off: Int = 0): Int {
                    require(byteArray.size - off >= Int.SIZE_BYTES)
                    var n: Int = byteArray[off].toInt() shl 24
                    n = n or (byteArray[off + 1].toInt() and 0xff).shl(16)
                    n = n or (byteArray[off + 2].toInt() and 0xff).shl(8)
                    n = n or (byteArray[off + 3].toInt() and 0xff)
                    return n
                }

                private fun byteArrayToLongBE(byteArray: ByteArray, off: Int = 0): Long {
                    require(byteArray.size - off >= Long.SIZE_BYTES)
                    val hi = byteArrayToIntBE(byteArray, off)
                    val lo = byteArrayToIntBE(byteArray, off + 4)
                    return (hi.toLong() and 0xffffffffL) shl 32 or (lo.toLong() and 0xffffffffL)
                }
            }

            /**
             * Extra hop contained in RoutingInfoTag
             *
             * @param nodeId 264 bits. start of the channel
             * @param shortChannelId 64 bits. channel id
             * @param feeBase 32 bits, big-endian. node fixed fee
             * @param feeProportionalMillionths 32 bits, big-endian. node proportional fee
             * @param cltvExpiryDelta 16 bits, big-endian. node cltv expiry delta
             */
            data class ExtraHop(
                val nodeId: LightningNodePubKey,
                val shortChannelId: String,
                val feeBase: MilliSat,
                val feeProportionalMillionths: Long,
                val cltvExpiryDelta: Int
            )
        }

        /**
         * Unknown tag (may or may not be valid)
         */
        data class UnknownTag(override val tag: Int5, val value: List<Int5>) : TaggedField() {
            fun encode(): List<Int5> = value.toList()

            init {

            }
        }

        /**
         * Invalid tags that we failed to decode
         */
        data class InvalidTag(override val tag: Int5, val value: List<Int5>) : TaggedField() {
            fun encode(): List<Int5> = value.toList()
        }
    }

    fun getSatsAmount(): Sat? {
        amount?.let { amount ->
            return amount.toSat()
        }
        return null
    }

    fun getMemo(): String {
        tags.forEach { taggedField ->
            if (taggedField is TaggedField.Description) {
                return taggedField.description
            }
        }
        return "-"
    }
    fun getExpiryTime(): Long? {
        tags.forEach { taggedField ->
            if (taggedField is TaggedField.Expiry) {
                return taggedField.expirySeconds
            }
        }
        return null // Return null if no expiry tag is found
    }

}