package chat.sphinx.concept_network_client_crypto

inline val String.isCryptoHeader: Boolean
    get() = startsWith(CryptoScheme.CRYPTO + CryptoScheme.DELIMITER)

/**
 * Constructs a header value to be added to a network request for encrypting/decrypting
 * data coming off the wire. This is used in concert with an OkHttp Interceptor whereby
 * the header will be removed prior to the request being sent, and the [value] is used
 * to encrypt/decrypt the request body.
 * */
sealed class CryptoHeader(val key: String, val value: String) {

    /**
     * Constructs a header for encrypting the request body of an OkHttp network call.
     * */
    class Encrypt private constructor(
        key: String,
        password: String,
    ): CryptoHeader(key, password) {

        class Builder: CryptoHeader.Builder<Encrypt, CryptoScheme.Encrypt>() {
            override fun build(): Encrypt {
                return Encrypt(key, value)
            }
        }

    }

    /**
     * Constructs a header for decrypting the response body of an OkHttp network call.
     * */
    class Decrypt private constructor(
        key: String,
        password: String,
    ): CryptoHeader(key, password) {

        class Builder: CryptoHeader.Builder<Decrypt, CryptoScheme.Decrypt>() {
            override fun build(): Decrypt {
                return Decrypt(key, value)
            }
        }

    }

    companion object {
        @Suppress("ObjectPropertyName")
        private const val _17 = 17
        @Suppress("ObjectPropertyName")
        private const val _31 = 31
    }

    override fun equals(other: Any?): Boolean {
        return  other       is CryptoHeader &&
                other.key   == key          &&
                other.value == value
    }

    override fun hashCode(): Int {
        var result = _17
        result = _31 * result + key.hashCode()
        result = _31 * result + value.hashCode()
        return result
    }

    override fun toString(): String {
        return "${this.javaClass.simpleName}(key=$key,value=$value)"
    }

    abstract class Builder<T: CryptoHeader, V: CryptoScheme> internal constructor() {
        protected var key: String = ""
            private set
        protected var value: String = ""
            private set

        fun setScheme(scheme: V) = apply {
            when (scheme) {
                is CryptoScheme.Encrypt -> {
                    key = CryptoScheme.CRYPTO +
                            CryptoScheme.DELIMITER +
                            CryptoScheme.Encrypt.ENC +
                            CryptoScheme.DELIMITER +
                            scheme.name
                }
                is CryptoScheme.Decrypt -> {
                    key =  CryptoScheme.CRYPTO +
                            CryptoScheme.DELIMITER +
                            CryptoScheme.Decrypt.DEC +
                            CryptoScheme.DELIMITER +
                            scheme.name
                }
                else -> {}
            }
        }

        fun setPassword(password: String) = apply {
            value = password
        }

        @Throws(IllegalArgumentException::class)
        abstract fun build(): T
    }
}
