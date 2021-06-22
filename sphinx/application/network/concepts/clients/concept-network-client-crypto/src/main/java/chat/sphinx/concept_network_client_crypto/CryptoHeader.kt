package chat.sphinx.concept_network_client_crypto

inline val String.isCryptoHeader: Boolean
    get() = startsWith(CryptoScheme.CRYPTO + CryptoScheme.DELIMITER)

sealed class CryptoHeader(val key: String, val value: String) {

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

    class Decrypt private constructor(
        key: String,
        value: String,
    ): CryptoHeader(key, value) {

        class Builder: CryptoHeader.Builder<Decrypt, CryptoScheme.Decrypt>() {
            override fun build(): Decrypt {
                return Decrypt(key, value)
            }
        }

    }

    init {
        require(key.isNotEmpty()) {
            "HeaderEncrypt.key cannot be empty"
        }
        require(value.isNotEmpty()) {
            "HeaderEncrypt.value cannot be empty"
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
