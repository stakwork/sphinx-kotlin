package chat.sphinx.concept_crypto.rsa

inline class RsaPrivateKey(val value: CharArray)
inline class RsaPublicKey(val value: CharArray)

class RSAKeyPair(
    val privateKey: RsaPrivateKey,
    val publicKey: RsaPublicKey,
)
