package chat.sphinx.concept_crypto.rsa

inline class RsaPrivateKey(val value: CharArray)
inline class RsaPublicKey(val value: CharArray)

@Suppress("NOTHING_TO_INLINE")
inline fun RsaPrivateKey.formatToCert(singleLine: Boolean = false): String {
    val sb = StringBuilder()

    sb.append("-----BEGIN RSA PRIVATE KEY-----\n")

    if (!singleLine) {
        sb.append(value.joinToString("").replace("(.{64})".toRegex(), "$1\n"))
    } else {
        sb.append(value.joinToString(""))
    }

    sb.append("\n-----END RSA PRIVATE KEY-----")

    return sb.toString()
}

@Suppress("NOTHING_TO_INLINE")
inline fun RsaPublicKey.formatToCert(singleLine: Boolean = false): String {
    val sb = StringBuilder()

    sb.append("-----BEGIN RSA PUBLIC KEY-----\n")

    if (!singleLine) {
        sb.append(value.joinToString("").replace("(.{64})".toRegex(), "$1\n"))
    } else {
        sb.append(value.joinToString(""))
    }

    sb.append("\n-----END RSA PUBLIC KEY-----")

    return sb.toString()
}

class RSAKeyPair(
    val privateKey: RsaPrivateKey,
    val publicKey: RsaPublicKey,
)
