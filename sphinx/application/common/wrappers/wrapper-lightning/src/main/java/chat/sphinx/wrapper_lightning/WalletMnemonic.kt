package chat.sphinx.wrapper_lightning

@Suppress("NOTHING_TO_INLINE")
inline fun String.toWalletMnemonic(): WalletMnemonic? =
    try {
        WalletMnemonic(this)
    } catch (e: IllegalArgumentException) {
        null
    }

inline val String.isValidWalletMnemonic: Boolean
    get() {
        val wordCount = this.split(" ").size
        return wordCount == 12 || wordCount == 24
    }

@JvmInline
value class WalletMnemonic(val value: String){
    init {
        require(value.isNotEmpty() && value.isValidWalletMnemonic) {
            "WalletMnemonic cannot be empty"
        }
    }
}
