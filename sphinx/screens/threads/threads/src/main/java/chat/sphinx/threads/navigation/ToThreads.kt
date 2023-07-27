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

        val animation = NavOptions.Builder()
            .setEnterAnim(R.anim.slide_in_left)
            .setExitAnim(R.anim.slide_out_left)
            .setPopEnterAnim(R.anim.slide_in_right)
            .setPopExitAnim(R.anim.slide_out_right)

        controller.navigate(
            R.id.threads_nav_graph,
            args.build().toBundle(),
            animation.build()
        )
    }

}
