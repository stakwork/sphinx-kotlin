package chat.sphinx.feature_crypto_rsa

@Suppress("ClassName")
sealed class RSAAlgorithm {

    companion object {
        const val ALGORITHM_RSA = "RSA"
        const val ALGORITHM_RSA_ECB_PKCS1PADDING = "$ALGORITHM_RSA/ECB/PKCS1Padding"
    }

    abstract val value: String

    object RSA: RSAAlgorithm() {
        override val value: String
            get() = ALGORITHM_RSA
    }

    object RSA_ECB_PKCS1Padding: RSAAlgorithm() {
        override val value: String
            get() = ALGORITHM_RSA_ECB_PKCS1PADDING
    }
}