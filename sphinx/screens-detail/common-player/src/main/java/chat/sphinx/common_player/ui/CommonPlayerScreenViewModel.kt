package chat.sphinx.common_player.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.common_player.navigation.CommonPlayerNavigator
import chat.sphinx.common_player.viewstate.CommonPlayerScreenViewState
import chat.sphinx.wrapper_feed.FeedRecommendation
import chat.sphinx.wrapper_feed.toFeedRecommendationOrNull
import com.squareup.moshi.Moshi
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class CommonPlayerScreenViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    private val navigator: CommonPlayerNavigator,
    private val moshi: Moshi,
    savedStateHandle: SavedStateHandle,
): SideEffectViewModel<
        Context,
        CommonPlayerScreenSideEffect,
        CommonPlayerScreenViewState,
        >(dispatchers, CommonPlayerScreenViewState.Idle)
{

    private val args: CommonPlayerScreenFragmentArgs by savedStateHandle.navArgs()

    init {
        loadRecommendations()
    }

    private fun loadRecommendations() {
        var feedRecommendationList: MutableList<FeedRecommendation> = mutableListOf()
        var selectedRecommendation: FeedRecommendation? = null

        for (r in args.argRecommendations) {
            r.toFeedRecommendationOrNull(moshi)?.let { feedRecommendation ->
                feedRecommendationList.add(feedRecommendation)

                if (feedRecommendation.id == args.argRecommendationId) {
                    selectedRecommendation = feedRecommendation
                }
            }
        }

        selectedRecommendation?.let {
            updateViewState(
                CommonPlayerScreenViewState.FeedRecommendations(
                    feedRecommendationList,
                    it
                )
            )
        } ?: run {
            viewModelScope.launch(mainImmediate) {
                submitSideEffect(
                    CommonPlayerScreenSideEffect.Notify.ErrorLoadingRecommendations
                )

                navigator.closeDetailScreen()
            }
        }
    }
}