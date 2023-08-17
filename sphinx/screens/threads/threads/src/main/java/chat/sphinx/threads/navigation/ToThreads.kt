package chat.sphinx.threads.navigation

import androidx.annotation.IdRes
import androidx.navigation.NavController
import chat.sphinx.threads.R
import chat.sphinx.threads.ui.ThreadsFragmentArgs
import chat.sphinx.wrapper_common.dashboard.ChatId
import io.matthewnelson.android_feature_navigation.DefaultNavOptions
import io.matthewnelson.concept_navigation.NavigationRequest

class ToThreads(
    val chatId: ChatId,
    @IdRes private val popUpToId: Int? = null,
    private val popUpToInclusive: Boolean = false,
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        val args = ThreadsFragmentArgs.Builder(chatId.value)

        controller.navigate(
            R.id.threads_nav_graph,

            args.build().toBundle(),

            DefaultNavOptions.defaultAnims.let { builder ->
                popUpToId?.let { id ->
                    builder.setPopUpTo(id, popUpToInclusive)
                }
                builder.build()
            }
        )
    }

}
