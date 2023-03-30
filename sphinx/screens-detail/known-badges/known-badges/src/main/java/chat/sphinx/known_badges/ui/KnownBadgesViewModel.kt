package chat.sphinx.known_badges.ui

import android.app.Application
import android.content.*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_network_query_people.NetworkQueryPeople
import chat.sphinx.known_badges.navigation.KnownBadgesNavigator
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.wrapper_badge.Badge
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class KnownBadgesViewModel @Inject constructor(
    val navigator: KnownBadgesNavigator,
    dispatchers: CoroutineDispatchers,
    private val networkQueryPeople: NetworkQueryPeople,
    savedStateHandle: SavedStateHandle,
): SideEffectViewModel<
        Context,
        KnownBadgesSideEffect,
        KnownBadgesViewState
        >(
            dispatchers,
            KnownBadgesViewState.Idle,
        )
{

    private val args: KnownBadgesFragmentArgs by savedStateHandle.navArgs()

    init {
        loadKnownBadges()
    }

    private fun loadKnownBadges() {
        viewModelScope.launch(mainImmediate) {

            updateViewState(KnownBadgesViewState.Loading)

            val badgeIds = args.badgeIds.map {
                it.substringAfter("/").toLong()
            }

            networkQueryPeople.getKnownBadges(
                badgeIds.toTypedArray()
            ).collect { loadResponse ->
                when(loadResponse) {
                    is LoadResponse.Loading -> {}
                    is Response.Error -> {
                        updateViewState(
                            KnownBadgesViewState.KnownBadges(listOf())
                        )
                    }
                    is Response.Success -> {
                        updateViewState(
                            KnownBadgesViewState.KnownBadges(
                                loadResponse.value.map {
                                    Badge(
                                        name = it.name ?: "",
                                        description = it.memo ?: "",
                                        rewardType = it.reward_type ?: 0,
                                        rewardRequirement = it.reward_requirement,
                                        amountCreated = it.amount_created,
                                        amountIssued = it.amount_issued,
                                        isActive = it.active,
                                        imageUrl = it.icon ?: "",
                                        badgeId = it.badge_id,
                                        chatId = null,
                                        claimAmount = it.claim_amount
                                    )
                                }
                            )
                        )
                    }
                }
            }
        }
    }
}