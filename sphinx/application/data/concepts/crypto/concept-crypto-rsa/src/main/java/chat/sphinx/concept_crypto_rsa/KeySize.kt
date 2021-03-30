package chat.sphinx.concept_crypto_rsa

@Suppress("ClassName")
sealed class KeySize {

    companion object {
        const val KS_1024 = 1024
        const val KS_2048 = KS_1024 * 2
        const val KS_3072 = KS_1024 * 3
        const val KS_4096 = KS_1024 * 4
        const val KS_8192 = KS_1024 * 8
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

    class Custom(override val value: Int) : KeySize() {
        init {
            require(value in KS_1024..KS_8192 && value % KS_1024 == 0) {
                "\n" + """
                    KeySize must be:
                     - greater than or equal to $KS_1024
                     - less than or equal to $KS_8192
                     - divisible by $KS_1024
                """.trimIndent() + "\n"
            }
        }
    }
}
