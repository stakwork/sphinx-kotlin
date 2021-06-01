package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.chat_tribe.navigation.TribeChatNavigator
import chat.sphinx.chat_tribe.podcast_player.navigation.ToTribeChatPodcastPlayerDetail
import javax.inject.Inject

internal class TribeChatNavigatorImpl @Inject constructor(
    navigationDriver: PrimaryNavigationDriver,
    private val detailDriver: DetailNavigationDriver,
    ): TribeChatNavigator(navigationDriver)
{
    override suspend fun toTribeChatPodcastPlayerDetail() {
        detailDriver.submitNavigationRequest(ToTribeChatPodcastPlayerDetail())
    }
}
