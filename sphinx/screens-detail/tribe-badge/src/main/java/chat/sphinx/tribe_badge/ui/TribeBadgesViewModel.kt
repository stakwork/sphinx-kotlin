package chat.sphinx.tribe_badge.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_network_query_people.NetworkQueryPeople
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.tribe_badge.R
import chat.sphinx.tribe_badge.model.TribeBadgeHolder
import chat.sphinx.tribe_badge.model.TribeBadgeHolderType
import chat.sphinx.tribe_badge.navigation.TribeBadgesNavigator
import chat.sphinx.wrapper_common.dashboard.ChatId
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class TribeBadgesViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
    val navigator: TribeBadgesNavigator,
    private val networkQueryPeople: NetworkQueryPeople,
    ): SideEffectViewModel<
        Context,
        TribeBadgesSideEffect,
        TribeBadgesViewState>(dispatchers, TribeBadgesViewState.Idle)
{

    private val args: TribeBadgesFragmentArgs by savedStateHandle.navArgs()

    val chatId = args.argChatId

    fun goToCreateBadgeScreen(
        badgeName: String,
        badgeImage: String,
        badgeDescription: String,
        badgeAmount: String,
        badgeLeft: String,
        badgeActive: Boolean
    ) {
        viewModelScope.launch(mainImmediate) {
            navigator.toCreateBadgeScreen(
                badgeName,
                badgeImage,
                badgeDescription,
                badgeAmount,
                badgeLeft,
                badgeActive
            )
        }
    }

    init {
        getBadgesTemplates()
        }

    private fun getBadgesTemplates() {
        viewModelScope.launch(mainImmediate) {
            networkQueryPeople.getBadgeTemplates().collect { loadResponse ->
                when (loadResponse) {
                    is LoadResponse.Loading -> {
                        updateViewState(TribeBadgesViewState.Loading)
                    }
                    is Response.Error -> {
                        updateViewState(TribeBadgesViewState.Close)
                    }
                    is Response.Success -> {
                        val badgeTemplatesList: List<TribeBadgeHolder> = loadResponse.value.map {
                            TribeBadgeHolder(
                                name = it.name ?: "",
                                imageUrl = it.icon,
                                rewardType = if (it.rewardType == 1) R.string.badges_earn else R.string.badges_spend,
                                rewardRequirement = it.rewardRequirement,
                                holderType = TribeBadgeHolderType.TEMPLATE
                            )
                        }
                        networkQueryPeople.getUserExistingBadges(ChatId(chatId)).collect { existingBadges ->
                            when (existingBadges) {
                                is Response.Error -> {
                                    updateViewState(TribeBadgesViewState.TribeBadgesList(badgeTemplatesList))
                                }
                                is LoadResponse.Loading -> {}
                                is Response.Success -> {
                                    val manageBadgeLabel = TribeBadgeHolder(
                                        name = "Manage Label",
                                        holderType = TribeBadgeHolderType.HEADER
                                    )

                                    val existingBadgesList: List<TribeBadgeHolder> = existingBadges.value.map {
                                        TribeBadgeHolder(
                                            name = it.name ?: "",
                                            description = it.memo,
                                            rewardType = if (it.reward_type == 1) R.string.badges_earn else R.string.badges_spend,
                                            rewardRequirement = it.reward_requirement,
                                            amount_created = it.amount_created,
                                            amount_issued = it.amount_issued,
                                            isActive = it.activationState,
                                            imageUrl = it.icon,
                                            holderType = TribeBadgeHolderType.BADGE
                                        )
                                    }.sortedWith(
                                        compareByDescending<TribeBadgeHolder> { it.isActive }
                                            .thenBy { it.name }
                                    )
                                    val badgesList = badgeTemplatesList.plus(manageBadgeLabel).plus(existingBadgesList)
                                    updateViewState(TribeBadgesViewState.TribeBadgesList(badgesList))
                                }
                            }
                        }
                    }
                }
            }
        }
    }


}
