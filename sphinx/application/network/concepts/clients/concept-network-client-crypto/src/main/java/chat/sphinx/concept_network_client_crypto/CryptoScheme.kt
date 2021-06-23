package chat.sphinx.concept_network_client_crypto

/**
 * If the header key is a [CryptoHeader], will return the [CryptoScheme] associated
 * with it, or null if it is not a [CryptoHeader].
 *
 * @throws [IllegalArgumentException] if the [CryptoScheme] associated with the header key
 *   is not recognized.
 * */
@Suppress("NOTHING_TO_INLINE")
@Throws(IllegalArgumentException::class)
inline fun String.retrieveCryptoScheme(): CryptoScheme? {
    if (isCryptoHeader) {
        val splits = split(CryptoScheme.DELIMITER)
        val encOrDec = splits[1]
        val name = splits[2]

        if (encOrDec == CryptoScheme.Decrypt.DEC) {
            return when (name) {
                CryptoScheme.Decrypt.JNCryptor.name -> {
                    CryptoScheme.Decrypt.JNCryptor
                }
                else -> {
                    throw IllegalArgumentException(
                        """
                        Unrecognized decryption scheme for header key: $this
                        '${CryptoScheme.Decrypt.DEC + CryptoScheme.DELIMITER}' was expressed but
                        decryption scheme '$name' was invalid.
                    """.trimIndent()
                    )
                }
            }
        }

        if (encOrDec == CryptoScheme.Encrypt.ENC) {
            return when (name) {
                CryptoScheme.Encrypt.JNCryptor.name -> {
                    CryptoScheme.Encrypt.JNCryptor
                }
                else -> {
                    throw IllegalArgumentException(
                        """
                        Unrecognized encryption scheme for header key: $this
                        '${CryptoScheme.Encrypt.ENC + CryptoScheme.DELIMITER}' was expressed but
                        encryption scheme '$name' was invalid.
                    """.trimIndent()
                    )
                }
            }
        }

        throw IllegalArgumentException(
            """
                Failed to analyze header key for: $this
                Header.key started with ${CryptoScheme.CRYPTO}, but
                encrypt/decrypt was not specified in the second argument.
            """.trimIndent()
        )
    }

    return null
}

sealed class CryptoScheme {

    companion object {
        const val CRYPTO = "CRYPTO"
        const val DELIMITER = "-"
    }

    val name: String
        get() = this.javaClass.simpleName

    sealed class Decrypt: CryptoScheme() {

        companion object {
            const val DEC = "DEC"
        }

        object JNCryptor: Decrypt()
    }

    sealed class Encrypt: CryptoScheme() {

        companion object {
            const val ENC = "ENC"
        }

        object JNCryptor: Encrypt()
    }
}
