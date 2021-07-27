package chat.sphinx.onboard_ready.navigation

import androidx.annotation.IdRes
import androidx.navigation.NavController
import chat.sphinx.onboard_common.model.OnBoardInviterData
import chat.sphinx.onboard_common.model.OnBoardStep
import chat.sphinx.onboard_ready.R
import chat.sphinx.onboard_ready.ui.OnBoardReadyFragmentArgs
import chat.sphinx.wrapper_common.lightning.toLightningNodePubKey
import io.matthewnelson.android_feature_navigation.DefaultNavOptions
import io.matthewnelson.concept_navigation.NavigationRequest

internal inline val OnBoardReadyFragmentArgs.inviterData: OnBoardInviterData
    get() = OnBoardInviterData(
        argNickname?.ifEmpty { null },
        argPubkey?.toLightningNodePubKey(),
        argRouteHint?.ifEmpty { null },
        argMessage?.ifEmpty { null },
        argAction?.ifEmpty { null },
        argPin?.ifEmpty { null },
    )

class ToOnBoardReadyScreen(
    @IdRes private val popUpToId: Int,
    private val onBoardStep: OnBoardStep.Step4_Ready,
): NavigationRequest<NavController>() {

    override fun navigate(controller: NavController) {
        OnBoardReadyFragmentArgs.Builder(
            onBoardStep.inviterData.nickname,
            onBoardStep.inviterData.pubkey?.value,
            onBoardStep.inviterData.routeHint,
            onBoardStep.inviterData.message,
            onBoardStep.inviterData.action,
            onBoardStep.inviterData.pin,
        ).build().let { args ->

            controller.navigate(
                R.id.on_board_ready_nav_graph,
                args.toBundle(),
                DefaultNavOptions.defaultAnims
                    .setPopUpTo(popUpToId, false)
                    .build()
            )

        }
    }

}
