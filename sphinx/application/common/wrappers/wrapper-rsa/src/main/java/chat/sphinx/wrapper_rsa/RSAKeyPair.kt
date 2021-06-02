package chat.sphinx.wrapper_rsa

@Suppress("NOTHING_TO_INLINE")
inline fun RsaPrivateKey.formatToCert(pkcsType: PKCSType, singleLine: Boolean = false): String {
    val sb = StringBuilder()

    if (pkcsType is PKCSType.PKCS1) {
        sb.append("${RsaPrivateKey.CERT_PKCS1_HEADER}\n")
    } else {
        sb.append("${RsaPrivateKey.CERT_PKCS8_HEADER}\n")
    }

    if (!singleLine) {
        sb.append(value.joinToString("").replace("(.{64})".toRegex(), "$1\n"))
    } else {
        sb.append(value.joinToString(""))
    }

    if (pkcsType is PKCSType.PKCS1) {
        sb.append("\n${RsaPrivateKey.CERT_PKCS1_FOOTER}")
    } else {
        sb.append("\n${RsaPrivateKey.CERT_PKCS8_FOOTER}")
    }

    return sb.toString()
}

@Suppress("NOTHING_TO_INLINE")
inline fun RsaPublicKey.formatToCert(pkcsType: PKCSType, singleLine: Boolean = false): String {
    val sb = StringBuilder()

    if (pkcsType is PKCSType.PKCS1) {
        sb.append("${RsaPublicKey.CERT_PKCS1_HEADER}\n")
    } else {
        sb.append("${RsaPublicKey.CERT_PKCS8_HEADER}\n")
    }

    if (!singleLine) {
        sb.append(value.joinToString("").replace("(.{64})".toRegex(), "$1\n"))
    } else {
        sb.append(value.joinToString(""))
    }

    if (pkcsType is PKCSType.PKCS1) {
        sb.append("\n${RsaPublicKey.CERT_PKCS1_FOOTER}")
    } else {
        sb.append("\n${RsaPublicKey.CERT_PKCS8_FOOTER}")
    }

    return sb.toString()
}

inline val String.isRsaPKCS1PrivateKeyCert: Boolean
    get() = contains(RsaPrivateKey.CERT_PKCS1_HEADER) && contains(RsaPrivateKey.CERT_PKCS1_FOOTER)

inline val String.isRsaPKCS8PrivateKeyCert: Boolean
    get() = contains(RsaPrivateKey.CERT_PKCS8_HEADER) && contains(RsaPrivateKey.CERT_PKCS8_FOOTER)

inline val String.isRsaPrivateKeyCert: Boolean
    get() = isRsaPKCS1PrivateKeyCert || isRsaPKCS8PrivateKeyCert

inline val String.isRsaPKCS1PublicKeyCert: Boolean
    get() = contains(RsaPublicKey.CERT_PKCS1_HEADER) && contains(RsaPublicKey.CERT_PKCS1_FOOTER)

inline val String.isRsaPKCS8PublicKeyCert: Boolean
    get() = contains(RsaPublicKey.CERT_PKCS8_HEADER) && contains(RsaPublicKey.CERT_PKCS8_FOOTER)

inline val String.isRsaPublicKeyCert: Boolean
    get() = isRsaPKCS1PublicKeyCert || isRsaPKCS8PublicKeyCert

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
    val sb = StringBuilder()
    var string = ""

    when {
        isRsaPKCS1PrivateKeyCert -> {
            string = replace(RsaPrivateKey.CERT_PKCS1_HEADER, "")
            string = string.replace(RsaPrivateKey.CERT_PKCS1_FOOTER, "")
        }
        isRsaPKCS8PrivateKeyCert -> {
            string = replace(RsaPrivateKey.CERT_PKCS8_HEADER, "")
            string = string.replace(RsaPrivateKey.CERT_PKCS8_FOOTER, "")
        }
        else -> {
            throw IllegalArgumentException("String value does not contain Private Key Header or Footer")
        }
    }

    for (line in string.lines()) {
        sb.append(line.trim())
    }

    return RsaPrivateKey(sb.toString().toCharArray())
}

@Suppress("NOTHING_TO_INLINE")
@Throws(IllegalArgumentException::class)
inline fun String.toRsaPublicKey(): RsaPublicKey {
    val sb = StringBuilder()
    var string = ""

    when {
        isRsaPKCS1PublicKeyCert -> {
            string = replace(RsaPublicKey.CERT_PKCS1_HEADER, "")
            string = string.replace(RsaPublicKey.CERT_PKCS1_FOOTER, "")
        }
        isRsaPKCS8PublicKeyCert -> {
            string = replace(RsaPublicKey.CERT_PKCS8_HEADER, "")
            string = string.replace(RsaPublicKey.CERT_PKCS8_FOOTER, "")
        }
        else -> {
            throw IllegalArgumentException("String value does not contain Public Key Header or Footer")
        }
    }

    for (line in string.lines()) {
        sb.append(line.trim())
    }

    return RsaPublicKey(sb.toString().toCharArray())
}

@JvmInline
value class RsaPrivateKey(val value: CharArray) {
    companion object {
        const val CERT_PKCS1_HEADER = "-----BEGIN RSA PRIVATE KEY-----"
        const val CERT_PKCS1_FOOTER = "-----END RSA PRIVATE KEY-----"
        const val CERT_PKCS8_HEADER = "-----BEGIN PRIVATE KEY-----"
        const val CERT_PKCS8_FOOTER = "-----END PRIVATE KEY-----"
    }
}

@JvmInline
value class RsaPublicKey(val value: CharArray) {
    companion object {
        const val CERT_PKCS1_HEADER = "-----BEGIN RSA PUBLIC KEY-----"
        const val CERT_PKCS1_FOOTER = "-----END RSA PUBLIC KEY-----"
        const val CERT_PKCS8_HEADER = "-----BEGIN PUBLIC KEY-----"
        const val CERT_PKCS8_FOOTER = "-----END PUBLIC KEY-----"
    }
}

class RSAKeyPair(
    val privateKey: RsaPrivateKey,
    val publicKey: RsaPublicKey,
)
