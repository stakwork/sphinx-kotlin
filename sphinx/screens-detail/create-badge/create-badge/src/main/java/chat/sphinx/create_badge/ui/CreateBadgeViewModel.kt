package chat.sphinx.create_badge.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.concept_network_query_people.NetworkQueryPeople
import chat.sphinx.concept_network_query_people.model.BadgeCreateDto
import chat.sphinx.concept_network_query_people.model.BadgeStateDto
import chat.sphinx.create_badge.R
import chat.sphinx.create_badge.navigation.CreateBadgeNavigator
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.wrapper_badge.Badge
import chat.sphinx.wrapper_badge.BadgeTemplate
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject


internal inline val CreateBadgeFragmentArgs.badge: Badge
    get() {
        return Badge(
            this.argName,
            this.argDescription,
            this.argBadgeId,
            this.argImage,
            this.argAmountCreated,
            this.argAmountIssued,
            this.argChatId,
            this.argClaimAmount,
            this.argRewardType,
            this.argRewardRequirement,
            this.argIsActive
        )
    }

internal inline val CreateBadgeFragmentArgs.template: BadgeTemplate
    get() {
        return BadgeTemplate(
            this.argName,
            this.argDescription,
            this.argRewardType,
            this.argRewardRequirement,
            this.argImage,
            this.argChatId
        )
    }

@HiltViewModel
internal class CreateBadgeViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
    val navigator: CreateBadgeNavigator,
    private val networkQueryPeople: NetworkQueryPeople
    ): SideEffectViewModel<
        Context,
        CreateBadgeSideEffect,
        CreateBadgeViewState>(dispatchers, CreateBadgeViewState.Idle)
{
    companion object {
        const val DEFAULT_QUANTITY: Int = 100
        const val DEFAULT_PRICE_PER_BADGE: Int = 10

        const val TYPE_TEMPLATE = 0
        const val TYPE_BADGE = 1
    }

    private val args: CreateBadgeFragmentArgs by savedStateHandle.navArgs()

    val imageLoaderDefaults by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_tribe)
            .transformation(Transformation.CircleCrop)
            .build()
    }

    init {
        if (args.argHolderType == TYPE_BADGE) {
            updateViewState(CreateBadgeViewState.ToggleBadge(args.badge))
        } else {
            updateViewState(
                CreateBadgeViewState.CreateBadge(
                    args.template,
                    DEFAULT_QUANTITY,
                    DEFAULT_PRICE_PER_BADGE
                )
            )
        }
    }

    fun toggleBadgeState() {
        (currentViewState as? CreateBadgeViewState.ToggleBadge)?.badge?.let { badge ->
            val updatedBadge = badge.getToggledBadge()

            updateViewState(CreateBadgeViewState.ToggleBadge(updatedBadge))

            updatedBadge.badgeId?.let { nnBadgeId ->
                updatedBadge.chatId?.let { nnChatId ->
                    viewModelScope.launch(mainImmediate) {
                        networkQueryPeople.changeBadgeState(
                            BadgeStateDto(
                                nnBadgeId,
                                nnChatId
                            ),
                            updatedBadge.isActive
                        ).collect { loadResponse ->
                            @Exhaustive
                            when (loadResponse) {
                                is Response.Error -> {
                                    updateViewState(CreateBadgeViewState.ToggleBadge(badge))
                                    submitSideEffect(CreateBadgeSideEffect.Notify.FailedToChangeState)
                                }
                                is Response.Success -> {}
                                is LoadResponse.Loading -> {}
                            }
                        }
                    }
                    return
                }
            }

            updateViewState(CreateBadgeViewState.ToggleBadge(badge))
        }
    }

    fun decreaseQuantity() {
        (currentViewState as? CreateBadgeViewState.CreateBadge)?.let { viewState ->
            if (viewState.currentQuantity > 0) {
                updateViewState(
                    CreateBadgeViewState.CreateBadge(
                        viewState.badgeTemplate,
                        currentQuantity = viewState.currentQuantity - 1,
                        DEFAULT_PRICE_PER_BADGE
                    )
                )
            }
        }
    }

    fun increaseQuantity() {
        (currentViewState as? CreateBadgeViewState.CreateBadge)?.let { viewState ->
            updateViewState(
                CreateBadgeViewState.CreateBadge(
                    viewState.badgeTemplate,
                    currentQuantity = viewState.currentQuantity + 1,
                    DEFAULT_PRICE_PER_BADGE
                )
            )
        }
    }

    fun createBadge(
        amount: Int,
        description: String,
    ) {
        viewModelScope.launch(mainImmediate) {
            (currentViewState as? CreateBadgeViewState.CreateBadge)?.let {

                updateViewState(CreateBadgeViewState.LoadingCreateBadge)

                networkQueryPeople.createBadge(
                    BadgeCreateDto(
                        chat_id = it.badgeTemplate.chatId,
                        name = it.badgeTemplate.name,
                        reward_requirement = it.badgeTemplate.rewardRequirement,
                        memo = description,
                        icon = it.badgeTemplate.imageUrl,
                        reward_type = it.badgeTemplate.rewardType,
                        active = false,
                        amount = amount
                    )
                ).collect { loadResponse ->
                    when (loadResponse) {
                        is Response.Error -> {
                            submitSideEffect(CreateBadgeSideEffect.Notify.FailedToCreateBadge)
                            updateViewState(it)
                        }
                        is Response.Success -> {
                            navigator.popBackStack()
                        }
                        is LoadResponse.Loading -> {}
                    }
                }
            }
        }
    }
}
