package chat.sphinx.concept_crypto.rsa

import okio.base64.decodeBase64ToArray
import okio.base64.encodeBase64

@Suppress("NOTHING_TO_INLINE")
inline fun RsaSignatureString.toRsaSignature(): RsaSignature? =
    value.decodeBase64ToArray()?.let { RsaSignature(it) }

inline class RsaSignatureString(val value: String)

@Suppress("NOTHING_TO_INLINE")
inline fun RsaSignature.toRsaSignatureString(): RsaSignatureString =
    RsaSignatureString(value.encodeBase64())

inline class RsaSignature(val value: ByteArray) {
    override fun toString(): String {
        return "RsaSignature(value=${value.encodeBase64()})"
    }
}

class RsaSignedString(
    val text: String,
    val signature: RsaSignature,
) {
    override fun toString(): String {
        return "RsaSignedString(text=$text, signature=$signature)"
    }
}
