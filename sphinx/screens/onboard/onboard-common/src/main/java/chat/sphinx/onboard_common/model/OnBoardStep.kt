package chat.sphinx.onboard_common.model

import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl

@Suppress("DataClassPrivateConstructor")
sealed class OnBoardStep {

    data class Step1 private constructor(
        val relayUrl: RelayUrl,
        val authorizationToken: AuthorizationToken,
        val inviterData: OnBoardInviterData
    ): OnBoardStep() {

        companion object {
            @JvmSynthetic
            internal operator fun invoke(
                relayUrl: RelayUrl,
                authorizationToken: AuthorizationToken,
                inviterData: OnBoardInviterData
            ) : Step1 =
                Step1(relayUrl, authorizationToken, inviterData)
        }

    }

    data class Step2 private constructor(val inviterData: OnBoardInviterData): OnBoardStep() {

        companion object {
            @JvmSynthetic
            internal operator fun invoke(inviterData: OnBoardInviterData): Step2 =
                Step2(inviterData)
        }
    }

    data class Step3 private constructor(val inviterData: OnBoardInviterData): OnBoardStep() {

        companion object {
            @JvmSynthetic
            internal operator fun invoke(inviterData: OnBoardInviterData): Step3 =
                Step3(inviterData)
        }
    }

    data class Step4 private constructor(val inviterData: OnBoardInviterData): OnBoardStep() {

        companion object {
            @JvmSynthetic
            internal operator fun invoke(inviterData: OnBoardInviterData): Step4 =
                Step4(inviterData)
        }
    }

}
