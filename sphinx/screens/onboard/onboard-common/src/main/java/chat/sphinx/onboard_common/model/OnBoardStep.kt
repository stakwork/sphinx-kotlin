package chat.sphinx.onboard_common.model

import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayHMacKey
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_relay.TransportToken
import chat.sphinx.wrapper_rsa.RsaPublicKey

@Suppress("DataClassPrivateConstructor", "ClassName")
sealed class OnBoardStep {

    data class Step1_WelcomeMessage private constructor(
        val relayUrl: RelayUrl,
        val authorizationToken: AuthorizationToken,
        val transportKey: RsaPublicKey?,
        val hMacKey: RelayHMacKey?,
        val inviterData: OnBoardInviterData
    ): OnBoardStep() {

        companion object {
            @JvmSynthetic
            internal operator fun invoke(
                relayUrl: RelayUrl,
                authorizationToken: AuthorizationToken,
                transportKey: RsaPublicKey?,
                hMacKey: RelayHMacKey?,
                inviterData: OnBoardInviterData
            ) : Step1_WelcomeMessage =
                Step1_WelcomeMessage(relayUrl, authorizationToken, transportKey, hMacKey, inviterData)
        }

    }

    data class Step2_Name private constructor(val inviterData: OnBoardInviterData): OnBoardStep() {

        companion object {
            @JvmSynthetic
            internal operator fun invoke(inviterData: OnBoardInviterData): Step2_Name =
                Step2_Name(inviterData)
        }
    }

    data class Step3_Picture private constructor(val inviterData: OnBoardInviterData): OnBoardStep() {

        companion object {
            @JvmSynthetic
            internal operator fun invoke(inviterData: OnBoardInviterData): Step3_Picture =
                Step3_Picture(inviterData)
        }
    }

    data class Step4_Ready private constructor(val inviterData: OnBoardInviterData): OnBoardStep() {

        companion object {
            @JvmSynthetic
            internal operator fun invoke(inviterData: OnBoardInviterData): Step4_Ready =
                Step4_Ready(inviterData)
        }
    }

}
