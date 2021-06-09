package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.chat_tribe.navigation.TribeChatNavigator
import chat.sphinx.podcast_player.navigation.ToPodcastPlayerScreen
import chat.sphinx.podcast_player.objects.Podcast
import javax.inject.Inject

internal class TribeChatNavigatorImpl @Inject constructor(
    navigationDriver: PrimaryNavigationDriver,
    private val detailDriver: DetailNavigationDriver,
    ): TribeChatNavigator(navigationDriver)
{
    override suspend fun toPodcastPlayerScreen(podcast: Podcast) {
        detailDriver.submitNavigationRequest(ToPodcastPlayerScreen(podcast))
    }
}
