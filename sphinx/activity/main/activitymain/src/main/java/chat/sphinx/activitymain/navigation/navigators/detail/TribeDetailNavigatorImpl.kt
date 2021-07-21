package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.podcast_player.objects.Podcast
import chat.sphinx.tribe_detail.navigation.ToTribeDetailScreen
import chat.sphinx.tribe_detail.navigation.TribeDetailNavigator
import chat.sphinx.wrapper_common.dashboard.ChatId
import javax.inject.Inject

internal class TribeDetailNavigatorImpl @Inject constructor(
    val detailDriver: DetailNavigationDriver,
): TribeDetailNavigator(detailDriver) {
    override suspend fun closeDetailScreen() {
        detailDriver.closeDetailScreen()
    }

    override suspend fun toTribeDetailScreen(chatId: ChatId, podcast: Podcast?) {
        detailDriver.submitNavigationRequest(ToTribeDetailScreen(chatId, podcast))
    }
}
