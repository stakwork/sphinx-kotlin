package chat.sphinx.video_screen.ui

import chat.sphinx.wrapper_feed.FeedItem
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewState

abstract class VideoFeedViewModel<
        VS: ViewState<VS>
        >(dispatchers: CoroutineDispatchers, initialViewState: VS)
    : BaseViewModel<VS>(dispatchers, initialViewState)
{
    abstract fun videoItemSelected(item: FeedItem)
}