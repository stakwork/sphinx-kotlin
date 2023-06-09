package chat.sphinx.example.delete_chat_media_detail.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import chat.sphinx.delete.chat.media.detail.R
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.example.delete_chat_media_detail.ui.DeleteChatMediaDetailFragmentArgs
import chat.sphinx.wrapper_common.dashboard.ChatId
import io.matthewnelson.concept_navigation.NavigationRequest
import io.matthewnelson.android_feature_navigation.R as nav_R


class ToDeleteChatMediaDetail(
    private val chatId: ChatId,
    private val options: NavOptions = DetailNavOptions.defaultBuilt
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.delete_chat_media_detail_nav_graph,
            DeleteChatMediaDetailFragmentArgs.Builder(chatId.value).build().toBundle(),
            DetailNavOptions.default
                .setEnterAnim(nav_R.anim.slide_in_left)
                .setPopExitAnim(nav_R.anim.slide_out_right)
                .build()
        )
    }

}
