package chat.sphinx.video_screen.navigation

import androidx.navigation.NavController
import chat.sphinx.video_fullscreen.navigation.ToFullscreenVideoActivity
import chat.sphinx.wrapper_common.feed.FeedUrl
import chat.sphinx.wrapper_common.message.MessageId
import io.matthewnelson.android_feature_navigation.requests.PopBackStack
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class VideoScreenNavigator(
    detailNavigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(detailNavigationDriver) {
    abstract suspend fun closeDetailScreen()

    suspend fun popBackStack() {
        navigationDriver.submitNavigationRequest(PopBackStack())
    }

    suspend fun toFullscreenVideoActivity(
        messageId: MessageId,
        videoFilepath: String?,
        feedUrl: FeedUrl? = null,
        currentTime: Long = 0L
    ) {
        navigationDriver.submitNavigationRequest(
            ToFullscreenVideoActivity(
                messageId,
                videoFilepath,
                feedUrl,
                currentTime
            )
        )
    }
}


