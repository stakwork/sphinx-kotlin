package chat.sphinx.concept_crypto.rsa

@Suppress("ClassName")
sealed class KeySize {

    companion object {
        private const val KS_1024 = 1024
        private const val KS_2048 = 2048
        private const val KS_3072 = 3072
        private const val KS_4096 = 4096
        private const val KS_8192 = 8192
    }

    abstract val value: Int

    object _1024: KeySize() {
        override val value: Int
            get() = KS_1024
    }

    object _2048: KeySize() {
        override val value: Int
            get() = KS_2048
    }

    object _3072: KeySize() {
        override val value: Int
            get() = KS_3072
    }

    object _4096: KeySize() {
        override val value: Int
            get() = KS_4096
    }

    object _8192: KeySize() {
        override val value: Int
            get() = KS_8192
    }
}
