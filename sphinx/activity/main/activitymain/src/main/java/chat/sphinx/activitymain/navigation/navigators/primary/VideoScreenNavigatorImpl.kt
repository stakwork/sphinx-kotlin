package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.episode_description.navigation.ToEpisodeDescription
import chat.sphinx.episode_detail.navigation.ToEpisodeDetail
import chat.sphinx.video_screen.navigation.VideoScreenNavigator
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedUrl
import javax.inject.Inject

internal class VideoScreenNavigatorImpl @Inject constructor(
    navigationDriver: DetailNavigationDriver,
): VideoScreenNavigator(navigationDriver) {

    override suspend fun closeDetailScreen() {
        (navigationDriver as DetailNavigationDriver).closeDetailScreen()
    }

    override suspend fun toEpisodeDescriptionScreen(feedId: FeedId) {
        (navigationDriver as DetailNavigationDriver).submitNavigationRequest(ToEpisodeDescription(feedId, false))
    }
    override suspend fun toEpisodeDetail(
        feedItemId: FeedId?,
        header: String,
        image: String,
        episodeTypeImage: Int,
        episodeTypeText: String,
        episodeDate: String,
        episodeDuration: String,
        downloaded: Boolean?,
        link: FeedUrl?
    ) {
        (navigationDriver as DetailNavigationDriver).submitNavigationRequest(
            ToEpisodeDetail(
                feedItemId,
                header,
                image,
                episodeTypeImage,
                episodeTypeText,
                episodeDate,
                episodeDuration,
                downloaded,
                link
        ))
    }
}
