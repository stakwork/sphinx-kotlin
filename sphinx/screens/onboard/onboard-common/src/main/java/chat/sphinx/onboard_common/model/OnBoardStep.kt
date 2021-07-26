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

}
