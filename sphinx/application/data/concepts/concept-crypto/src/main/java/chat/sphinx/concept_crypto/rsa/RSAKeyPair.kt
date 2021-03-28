package chat.sphinx.concept_crypto.rsa

@Suppress("NOTHING_TO_INLINE")
inline fun RsaPrivateKey.formatToCert(singleLine: Boolean = false): String {
    val sb = StringBuilder()

    sb.append("${RsaPrivateKey.CERT_HEADER}\n")

    if (!singleLine) {
        sb.append(value.joinToString("").replace("(.{64})".toRegex(), "$1\n"))
    } else {
        sb.append(value.joinToString(""))
    }

    sb.append("\n${RsaPrivateKey.CERT_FOOTER}")

    return sb.toString()
}

@Suppress("NOTHING_TO_INLINE")
inline fun RsaPublicKey.formatToCert(singleLine: Boolean = false): String {
    val sb = StringBuilder()

    sb.append("${RsaPublicKey.CERT_HEADER}\n")

    if (!singleLine) {
        sb.append(value.joinToString("").replace("(.{64})".toRegex(), "$1\n"))
    } else {
        sb.append(value.joinToString(""))
    }

    sb.append("\n${RsaPublicKey.CERT_FOOTER}")

    return sb.toString()
}

inline val String.isRsaPrivateKeyCert: Boolean
    get() = contains(RsaPrivateKey.CERT_HEADER) && contains(RsaPrivateKey.CERT_FOOTER)

inline val String.isRsaPublicKeyCert: Boolean
    get() = contains(RsaPublicKey.CERT_HEADER) && contains(RsaPublicKey.CERT_FOOTER)

@Suppress("NOTHING_TO_INLINE")
inline fun String.toRsaPrivateKeyOrNull(): RsaPrivateKey? =
    try {
        toRsaPrivateKey()
    } catch (e: IllegalArgumentException) {
        null
    }

@Suppress("NOTHING_TO_INLINE")
inline fun String.toRsaPublicKeyOrNull(): RsaPublicKey? =
    try {
        toRsaPublicKey()
    } catch (e: IllegalArgumentException) {
        null
    }

@Suppress("NOTHING_TO_INLINE")
@Throws(IllegalArgumentException::class)
inline fun String.toRsaPrivateKey(): RsaPrivateKey {
    if (!isRsaPrivateKeyCert) {
        throw IllegalArgumentException(
            "String value does not contain RSA Private Key Header or Footer"
        )
    }

    val sb = StringBuilder()

    var string = replace(RsaPrivateKey.CERT_HEADER, "")
    string = string.replace(RsaPrivateKey.CERT_FOOTER, "")

    for (line in string.lines()) {
        sb.append(line.trim())
    }

    return RsaPrivateKey(sb.toString().toCharArray())
}

@Suppress("NOTHING_TO_INLINE")
@Throws(IllegalArgumentException::class)
inline fun String.toRsaPublicKey(): RsaPublicKey {
    if (!isRsaPublicKeyCert) {
        throw IllegalArgumentException(
            "String value does not contain RSA Public Key Header or Footer"
        )
    }

    val sb = StringBuilder()

    var string = replace(RsaPublicKey.CERT_HEADER, "")
    string = string.replace(RsaPublicKey.CERT_FOOTER, "")

    for (line in string.lines()) {
        sb.append(line.trim())
    }

    return RsaPublicKey(sb.toString().toCharArray())
}

inline class RsaPrivateKey(val value: CharArray) {
    companion object {
        const val CERT_HEADER = "-----BEGIN RSA PRIVATE KEY-----"
        const val CERT_FOOTER = "-----END RSA PRIVATE KEY-----"
    }
}

inline class RsaPublicKey(val value: CharArray) {
    companion object {
        const val CERT_HEADER = "-----BEGIN RSA PUBLIC KEY-----"
        const val CERT_FOOTER = "-----END RSA PUBLIC KEY-----"
    }
}

class RSAKeyPair(
    val privateKey: RsaPrivateKey,
    val publicKey: RsaPublicKey,
)
