package chat.sphinx.wrapper_rsa

@Suppress("SpellCheckingInspection")
sealed class PKCSType {
    object PKCS1: PKCSType()
    object PKCS8: PKCSType()
}
