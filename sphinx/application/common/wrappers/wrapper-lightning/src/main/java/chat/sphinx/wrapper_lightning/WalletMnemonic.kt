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
        return this.split(" ").size == 12
    }

@JvmInline
value class WalletMnemonic(val value: String){
    init {
        require(value.isNotEmpty()) {
            "WalletMnemonic cannot be empty"
        }
    }
}
