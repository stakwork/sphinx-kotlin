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
import chat.sphinx.wrapper_badge.Badge
import chat.sphinx.wrapper_badge.BadgeTemplate
import chat.sphinx.wrapper_common.dashboard.ChatId
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
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

    fun getBadgesTemplates() {
        viewModelScope.launch(mainImmediate) {
            networkQueryPeople.getBadgeTemplates().collect { loadResponse ->
                when (loadResponse) {
                    is LoadResponse.Loading -> {
                        updateViewState(TribeBadgesViewState.Loading)
                    }
                    is Response.Error -> {
                        updateViewState(TribeBadgesViewState.Error)
                    }
                    is Response.Success -> {

                        val badgeTemplatesList: List<BadgeTemplate> = loadResponse.value.map {
                            BadgeTemplate(
                                name = it.name ?: "",
                                rewardType = it.rewardType ?: 1,
                                rewardRequirement = it.rewardRequirement ?: 0,
                                imageUrl = it.icon ?: "",
                                chatId = chatId.toInt()
                            )
                        }

                        networkQueryPeople.getUserExistingBadges(
                            ChatId(chatId)
                        ).collect { existingBadges ->
                            when (existingBadges) {
                                is Response.Error -> {
                                    updateViewState(
                                        TribeBadgesViewState.TribeBadgesList(
                                            badgeTemplatesList.map {
                                                TribeBadgeHolder(
                                                    holderType = TribeBadgeHolderType.TEMPLATE,
                                                    badgeTemplate = it
                                                )
                                            }
                                        )
                                    )
                                }
                                is LoadResponse.Loading -> {}
                                is Response.Success -> {
                                    val existingBadgesList: List<Badge> = existingBadges.value.map {
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
                                            chatId = chatId.toInt(),
                                            claimAmount = it.claim_amount
                                        )
                                    }.sortedWith(
                                        compareByDescending<Badge> { it.isActive }
                                            .thenBy { it.name }
                                    )

                                    val tribeBadgesHolderList: List<TribeBadgeHolder> = badgeTemplatesList.map { badgeTemplate ->
                                        TribeBadgeHolder(
                                            TribeBadgeHolderType.TEMPLATE,
                                            badgeTemplate = badgeTemplate
                                        )
                                    }.plus(
                                        TribeBadgeHolder(
                                            holderType = TribeBadgeHolderType.HEADER,
                                            headerTitle = "Manage Label",
                                        )
                                    ).plus(
                                        existingBadgesList.map { badge ->
                                            TribeBadgeHolder(
                                                holderType = TribeBadgeHolderType.BADGE,
                                                badge = badge
                                            )
                                        }
                                    )

                                    updateViewState(TribeBadgesViewState.TribeBadgesList(tribeBadgesHolderList))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun goToCreateBadgeScreen(
        name: String,
        description: String,
        image: String,
        rewardType: Int,
        rewardRequirement: Int,
        isActive: Boolean,
        chatId: Int,
        badgeId: Int,
        amountCreated: Int,
        amountIssued: Int,
        claimAmount: Int,
        holderType: Int
    ) {
        viewModelScope.launch(mainImmediate) {
            val existingBadges = (currentViewState as? TribeBadgesViewState.TribeBadgesList)
                ?.tribeBadgeHolders?.map { it.badge?.rewardType } ?: listOf()

            if (existingBadges.contains(rewardType)) {
                submitSideEffect(TribeBadgesSideEffect.BadgeAlreadyExists)
                return@launch
            }

            navigator.toCreateBadgeScreen(
                name,
                description,
                image,
                rewardType,
                rewardRequirement,
                isActive,
                chatId,
                badgeId,
                amountCreated,
                amountIssued,
                claimAmount,
                holderType
            )
        }
    }


}
