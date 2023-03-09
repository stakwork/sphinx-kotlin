package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.common_player.navigation.CommonPlayerNavigator
import chat.sphinx.episode_detail.navigation.ToEpisodeDetail
import chat.sphinx.podcast_player.navigation.PodcastPlayerNavigator
import javax.inject.Inject

internal class CommonPlayerNavigatorImpl @Inject constructor (
    detailDriver: DetailNavigationDriver,
): CommonPlayerNavigator(detailDriver) {
    override suspend fun closeDetailScreen() {
        (navigationDriver as DetailNavigationDriver).closeDetailScreen()
    }

    override suspend fun toEpisodeDetail(
        header: String,
        image: String,
        episodeTypeImage: Int,
        episodeTypeText: String,
        episodeDate: String,
        episodeDuration: String,
    ) {
        (navigationDriver as DetailNavigationDriver).submitNavigationRequest(
            ToEpisodeDetail(
                header,
                image,
                episodeTypeImage,
                episodeTypeText,
                episodeDate,
                episodeDuration
            ))
    }
}
