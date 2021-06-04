package chat.sphinx.concept_crypto_rsa

@Suppress("ClassName")
sealed class SignatureAlgorithm {

    companion object {
        const val SHA1_RSA = "SHA1withRSA"
        const val SHA256_RSA = "SHA256withRSA"
        const val SHA384_RSA = "SHA384withRSA"
        const val SHA512_RSA = "SHA512withRSA"
    }

    abstract val value: String

    object SHA1_with_RSA: SignatureAlgorithm() {
        override val value: String
            get() = SHA1_RSA
    }

    object SHA256_with_RSA: SignatureAlgorithm() {
        override val value: String
            get() = SHA256_RSA
    }

    object SHA384_with_RSA: SignatureAlgorithm() {
        override val value: String
            get() = SHA384_RSA
    }

    object SHA512_with_RSA: SignatureAlgorithm() {
        override val value: String
            get() = SHA512_RSA
    }
}
