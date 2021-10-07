package chat.sphinx.onboard_lightning.navigation

import androidx.annotation.IdRes
import androidx.navigation.NavController
import chat.sphinx.onboard_common.model.OnBoardInviterData
import chat.sphinx.onboard_common.model.OnBoardStep
import chat.sphinx.onboard_lightning.R
import chat.sphinx.onboard_lightning.ui.OnBoardLightningFragmentArgs
import chat.sphinx.wrapper_common.lightning.toLightningNodePubKey
import io.matthewnelson.android_feature_navigation.DefaultNavOptions
import io.matthewnelson.concept_navigation.NavigationRequest

internal inline val OnBoardLightningFragmentArgs.inviterData: OnBoardInviterData
    get() = OnBoardInviterData(
        argNickname?.ifEmpty { null },
        argPubkey?.toLightningNodePubKey(),
        argRouteHint?.ifEmpty { null },
        argMessage?.ifEmpty { null },
        argAction?.ifEmpty { null },
        argPin?.ifEmpty { null },
    )

class ToOnBoardLightningScreen(
    @IdRes private val popUpToId: Int,
    private val onBoardStep: OnBoardStep.Step2_Name,
): NavigationRequest<NavController>() {

    override fun navigate(controller: NavController) {
        OnBoardLightningFragmentArgs.Builder(
            onBoardStep.inviterData.nickname,
            onBoardStep.inviterData.pubkey?.value,
            onBoardStep.inviterData.routeHint,
            onBoardStep.inviterData.message,
            onBoardStep.inviterData.action,
            onBoardStep.inviterData.pin,
        ).build().let { args ->

            controller.navigate(
                R.id.on_board_lightning_nav_graph,
                args.toBundle(),
                DefaultNavOptions.defaultAnims
                    .setPopUpTo(popUpToId, false)
                    .build()
            )

        }
    }
}