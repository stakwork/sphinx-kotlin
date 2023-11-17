package chat.sphinx.onboard_name.navigation

import androidx.annotation.IdRes
import androidx.navigation.NavController
import chat.sphinx.onboard_common.model.OnBoardInviterData
import chat.sphinx.onboard_common.model.OnBoardStep
import chat.sphinx.onboard_name.R
import chat.sphinx.onboard_name.ui.OnBoardNameFragmentArgs
import chat.sphinx.wrapper_common.lightning.toLightningNodePubKey
import io.matthewnelson.android_feature_navigation.DefaultNavOptions
import io.matthewnelson.concept_navigation.NavigationRequest

internal inline val OnBoardNameFragmentArgs.inviterData: OnBoardInviterData
    get() = OnBoardInviterData(
        argNickname?.ifEmpty { null },
        argPubkey?.toLightningNodePubKey(),
        argRouteHint?.ifEmpty { null },
        argMessage?.ifEmpty { null },
        argAction?.ifEmpty { null },
        argPin?.ifEmpty { null },
    )

class ToOnBoardNameScreen(
    @IdRes private val popUpToId: Int,
    private val onBoardStep: OnBoardStep.Step2_Name?,
): NavigationRequest<NavController>() {

    override fun navigate(controller: NavController) {
        OnBoardNameFragmentArgs.Builder(
            onBoardStep?.inviterData?.nickname,
            onBoardStep?.inviterData?.pubkey?.value,
            onBoardStep?.inviterData?.routeHint,
            onBoardStep?.inviterData?.message,
            onBoardStep?.inviterData?.action,
            onBoardStep?.inviterData?.pin,
        ).build().let { args ->

            controller.navigate(
                R.id.on_board_name_nav_graph,
                args.toBundle(),
                DefaultNavOptions.defaultAnims
                    .setPopUpTo(popUpToId, false)
                    .build()
            )

        }
    }

}
