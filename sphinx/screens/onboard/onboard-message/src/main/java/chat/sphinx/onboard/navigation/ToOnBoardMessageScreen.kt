package chat.sphinx.onboard.navigation

import androidx.annotation.IdRes
import androidx.navigation.NavController
import chat.sphinx.onboard.R
import chat.sphinx.onboard.ui.OnBoardMessageFragmentArgs
import chat.sphinx.onboard_common.model.OnBoardInviterData
import chat.sphinx.onboard_common.model.OnBoardStep
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.lightning.toLightningNodePubKey
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayHMacKey
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_rsa.RsaPublicKey
import io.matthewnelson.android_feature_navigation.DefaultNavOptions
import io.matthewnelson.concept_navigation.NavigationRequest

internal inline val OnBoardMessageFragmentArgs.relayUrl: RelayUrl
    get() = RelayUrl(argRelayUrl)

internal inline val OnBoardMessageFragmentArgs.authorizationToken: AuthorizationToken
    get() = AuthorizationToken(argAuthorizationToken)

internal inline val OnBoardMessageFragmentArgs.transportKey: RsaPublicKey?
    get() = if (argTransportKey.isEmpty()) {
        null
    } else {
        RsaPublicKey(argTransportKey.toCharArray())
    }

internal inline val OnBoardMessageFragmentArgs.hMacKey: RelayHMacKey?
    get() = if (argHMacKey.isEmpty()) {
        null
    } else {
        RelayHMacKey(argHMacKey)
    }

internal inline val OnBoardMessageFragmentArgs.inviterData: OnBoardInviterData
    get() = OnBoardInviterData(
        argNickname?.ifEmpty { null },
        argPubkey?.toLightningNodePubKey(),
        argRouteHint?.ifEmpty { null },
        argMessage?.ifEmpty { null },
        argAction?.ifEmpty { null },
        argPin?.ifEmpty { null },
    )

class ToOnBoardScreen(
    @IdRes private val popUpToId: Int,
    private val onBoardStep: OnBoardStep.Step1_WelcomeMessage,
): NavigationRequest<NavController>() {

    override fun navigate(controller: NavController) {
        val transportKeyOrEmpty = onBoardStep.transportKey?.value?.joinToString("") ?: ""
        val hMacKeyOrEmpty = onBoardStep.hMacKey?.value ?: ""

        OnBoardMessageFragmentArgs.Builder(
            onBoardStep.relayUrl.value,
            onBoardStep.authorizationToken.value,
            transportKeyOrEmpty,
            hMacKeyOrEmpty,
            onBoardStep.inviterData.nickname,
            onBoardStep.inviterData.pubkey?.value,
            onBoardStep.inviterData.routeHint,
            onBoardStep.inviterData.message,
            onBoardStep.inviterData.action,
            onBoardStep.inviterData.pin,
        ).build().let { args ->

            controller.navigate(
                R.id.on_board_nav_graph,
                args.toBundle(),
                DefaultNavOptions.defaultAnims
                    .setPopUpTo(popUpToId, false)
                    .build()
            )

        }
    }

}
