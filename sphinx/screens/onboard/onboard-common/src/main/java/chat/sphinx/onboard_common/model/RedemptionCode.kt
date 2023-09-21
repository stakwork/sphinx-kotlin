package chat.sphinx.onboard_common.model

import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_relay.toRelayUrl
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.crypto_common.clazzes.Password
import io.matthewnelson.crypto_common.extensions.decodeToString
import kotlinx.coroutines.withContext
import okio.base64.decodeBase64ToArray
import org.cryptonode.jncryptor.AES256JNCryptor
import org.cryptonode.jncryptor.CryptorException
import kotlin.jvm.Throws

sealed class RedemptionCode {

    companion object {
        fun decode(code: String): RedemptionCode? {
            code.decodeBase64ToArray()
                ?.decodeToString()
                ?.split("::")
                ?.let { decodedSplit ->
                    if (decodedSplit.size == 3) {
                        if (decodedSplit.elementAt(0) == NodeInvite.DECODED_INDEX_0) {
                            return NodeInvite(
                                decodedSplit.elementAt(1).toRelayUrl() ?: return null,
                                decodedSplit.elementAt(2)
                            )
                        }
                    } else if (decodedSplit.size == 2) {
                        if (decodedSplit.elementAt(0) == AccountRestoration.DECODED_INDEX_0) {
                            return AccountRestoration(
                                decodedSplit.elementAt(1).decodeBase64ToArray() ?: return null
                            )
                        }
                    }
                }
            code.split("::").let { swarmSplit ->
                if (swarmSplit.size == 3) {
                    if (swarmSplit.elementAt(0) == SwarmConnect.DECODED_INDEX_0) {
                        return SwarmConnect(
                            swarmSplit.elementAt(1).toRelayUrl() ?: return null,
                            swarmSplit.elementAt(2)
                        )
                    }
                    if (swarmSplit.elementAt(0) == SwarmClaim.DECODED_INDEX_0) {
                        return SwarmClaim(
                            swarmSplit.elementAt(1).toRelayUrl() ?: return null,
                            swarmSplit.elementAt(2).decodeBase64ToArray()?.decodeToString() ?: return null
                        )
                    }
                }
            }

            if (code.startsWith(Glyph.REGEX)) {
                val parameters = code.substringAfter(Glyph.REGEX).split("&").mapNotNull { code ->
                    code.split("=").takeIf { it.size == 2 }?.let { pair ->
                        pair[0] to pair[1]
                    }
                }.toMap()

                val mqtt = parameters[Glyph.PARAM_MQTT] ?: return null
                val network = parameters[Glyph.PARAM_NETWORK] ?: return null
                val relay = parameters[Glyph.PARAM_RELAY] ?: return null

                return Glyph(mqtt, network, relay)
            }

            return null
        }
    }

        class AccountRestoration private constructor(
            private val byteArrayToDecrypt: ByteArray
        ) : RedemptionCode() {

            companion object {
                const val DECODED_INDEX_0 = "keys"

                @JvmSynthetic
                internal operator fun invoke(byteArrayToDecrypt: ByteArray): AccountRestoration =
                    AccountRestoration(byteArrayToDecrypt)
            }

            class DecryptedRestorationCode private constructor(
                val privateKey: Password,
                val publicKey: Password,
                val relayUrl: RelayUrl,
                val authorizationToken: AuthorizationToken,
            ) {
                companion object {
                    @JvmSynthetic
                    internal operator fun invoke(
                        privateKey: Password,
                        publicKey: Password,
                        relayUrl: RelayUrl,
                        authorizationToken: AuthorizationToken,
                    ): DecryptedRestorationCode =
                        DecryptedRestorationCode(
                            privateKey,
                            publicKey,
                            relayUrl,
                            authorizationToken
                        )
                }
            }

            @Throws(CryptorException::class, IllegalArgumentException::class)
            suspend fun decrypt(
                pin: CharArray,
                dispatchers: CoroutineDispatchers
            ): DecryptedRestorationCode {
                val decryptedSplits = withContext(dispatchers.default) {
                    AES256JNCryptor()
                        .decryptData(byteArrayToDecrypt, pin)
                        .decodeToString()
                        .split("::")
                }

                if (decryptedSplits.size != 4) {
                    throw IllegalArgumentException("Decrypted keys do not contain enough arguments")
                }

                return DecryptedRestorationCode(
                    privateKey = Password(decryptedSplits[0].toCharArray()),
                    publicKey = Password(decryptedSplits[1].toCharArray()),
                    relayUrl = RelayUrl(decryptedSplits[2]),
                    authorizationToken = AuthorizationToken(decryptedSplits[3]),
                )
            }
        }


        @Suppress("DataClassPrivateConstructor")
        data class NodeInvite private constructor(
            val ip: RelayUrl,
            val password: String,
        ) : RedemptionCode() {

            companion object {
                const val DECODED_INDEX_0 = "ip"

                @JvmSynthetic
                internal operator fun invoke(ip: RelayUrl, password: String): NodeInvite =
                    NodeInvite(ip, password)
            }

        }

        @Suppress("DataClassPrivateConstructor")
        data class SwarmConnect private constructor(
            val ip: RelayUrl,
            val pubKey: String,
        ) : RedemptionCode() {

            companion object {
                const val DECODED_INDEX_0 = "connect"

                @JvmSynthetic
                internal operator fun invoke(ip: RelayUrl, pubKey: String): SwarmConnect =
                    SwarmConnect(ip, pubKey)
            }
        }

        @Suppress("DataClassPrivateConstructor")
        data class SwarmClaim private constructor(
            val ip: RelayUrl,
            val token: String,
        ) : RedemptionCode() {

            companion object {
                const val DECODED_INDEX_0 = "claim"

                @JvmSynthetic
                internal operator fun invoke(ip: RelayUrl, token: String): SwarmClaim =
                    SwarmClaim(ip, token)
            }
        }

    @Suppress("DataClassPrivateConstructor")
    data class Glyph private constructor(
        val mqtt: String,
        val network: String,
        val relay: String,
    ) : RedemptionCode() {

        companion object {
            const val REGEX = "sphinx.chat://?action=glyph"
            const val PARAM_MQTT = "mqtt"
            const val PARAM_NETWORK = "network"
            const val PARAM_RELAY = "relay"

            @JvmSynthetic
            internal operator fun invoke(mqtt: String, network: String, relay: String): Glyph =
                Glyph(mqtt, network, relay)
        }
    }

}
