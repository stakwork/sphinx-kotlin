package chat.sphinx.threads.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.threads.R
import chat.sphinx.threads.ui.ThreadsFragmentArgs
import chat.sphinx.wrapper_common.dashboard.ChatId
import io.matthewnelson.concept_navigation.NavigationRequest

class ToThreads(
    val chatId: ChatId,
    private val options: NavOptions = DetailNavOptions.defaultBuilt
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        val args = ThreadsFragmentArgs.Builder(chatId.value)

        controller.navigate(
            R.id.threads_nav_graph,
            args.build().toBundle(),
            options
        )
    }

}
