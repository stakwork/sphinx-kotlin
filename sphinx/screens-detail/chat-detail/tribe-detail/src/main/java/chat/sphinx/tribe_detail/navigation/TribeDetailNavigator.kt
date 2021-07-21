package chat.sphinx.tribe_detail.navigation

import androidx.navigation.NavController
import chat.sphinx.podcast_player.objects.ParcelablePodcast
import chat.sphinx.wrapper_common.dashboard.ChatId
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class TribeDetailNavigator(
    navigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(navigationDriver) {
    abstract suspend fun closeDetailScreen()

    abstract suspend fun toTribeDetailScreen(chatId: ChatId, podcast: ParcelablePodcast?)
}
