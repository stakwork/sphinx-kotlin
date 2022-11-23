package chat.sphinx.common_player.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.common_player.navigation.CommonPlayerNavigator
import chat.sphinx.common_player.viewstate.BoostAnimationViewState
import chat.sphinx.common_player.viewstate.CommonPlayerScreenViewState
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_feed.FeedRecommendation
import chat.sphinx.wrapper_feed.toFeedRecommendationOrNull
import com.squareup.moshi.Moshi
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommonPlayerScreenViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    private val navigator: CommonPlayerNavigator,
    private val contactRepository: ContactRepository,
    private val moshi: Moshi,
    savedStateHandle: SavedStateHandle,
): SideEffectViewModel<
        Context,
        CommonPlayerScreenSideEffect,
        CommonPlayerScreenViewState,
        >(dispatchers, CommonPlayerScreenViewState.Idle)
{

    private val args: CommonPlayerScreenFragmentArgs by savedStateHandle.navArgs()

    val boostAnimationViewStateContainer: ViewStateContainer<BoostAnimationViewState> by lazy {
        ViewStateContainer(BoostAnimationViewState.Idle)
    }

    init {
        loadRecommendations()

        viewModelScope.launch(mainImmediate) {
            val owner = getOwner()

            boostAnimationViewStateContainer.updateViewState(
                BoostAnimationViewState.BoosAnimationInfo(
                    owner.photoUrl,
                    owner.tipAmount
                )
            )
        }
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
            itemSelected(it, feedRecommendationList)
        } ?: run {
            viewModelScope.launch(mainImmediate) {
                submitSideEffect(
                    CommonPlayerScreenSideEffect.Notify.ErrorLoadingRecommendations
                )

                navigator.closeDetailScreen()
            }
        }
    }

    fun itemSelected(
        feedRecommendation: FeedRecommendation,
        recommendations: List<FeedRecommendation>? = null
    ) {
        var feedRecommendationList: MutableList<FeedRecommendation> = mutableListOf()

        recommendations?.let {
            feedRecommendationList.addAll(it)
        } ?: run {
            (currentViewState as? CommonPlayerScreenViewState.FeedRecommendations)?.let {
                feedRecommendationList.addAll(it.recommendations)
            }
        }

        updateViewState(
            if (feedRecommendation.isPodcast) {
                CommonPlayerScreenViewState.FeedRecommendations.PodcastSelected(
                    feedRecommendationList,
                    feedRecommendation
                )
            } else if (feedRecommendation.isYouTubeVideo) {
                CommonPlayerScreenViewState.FeedRecommendations.YouTubeVideoSelected(
                    feedRecommendationList,
                    feedRecommendation
                )
            } else {
                CommonPlayerScreenViewState.Idle
            }
        )
    }



    private suspend fun getOwner(): Contact {
        return contactRepository.accountOwner.value.let { contact ->
            if (contact != null) {
                contact
            } else {
                var resolvedOwner: Contact? = null
                try {
                    contactRepository.accountOwner.collect { ownerContact ->
                        if (ownerContact != null) {
                            resolvedOwner = ownerContact
                            throw Exception()
                        }
                    }
                } catch (e: Exception) {
                }
                delay(25L)

                resolvedOwner!!
            }
        }
    }
}