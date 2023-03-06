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
    private val args: CreateBadgeFragmentArgs by savedStateHandle.navArgs()

    val imageLoaderDefaults by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_tribe)
            .transformation(Transformation.CircleCrop)
            .build()
    }

    init {
        if (args.argHolderType == 1 ) {
            updateViewState(CreateBadgeViewState.EditBadge(args.badge))
        }
        else
        {
            updateViewState(CreateBadgeViewState.Template(args.template))
        }
    }

    fun changeBadgeState(
        badgeId: Int?,
        chatId: Long?,
        state: Boolean,
    ) {
        if (chatId != null && badgeId != null) {
            viewModelScope.launch(mainImmediate) {
                networkQueryPeople.changeBadgeState(
                    BadgeStateDto(
                        badgeId,
                        chatId
                    ),
                    state
                ).collect { loadResponse ->
                    @Exhaustive
                    when (loadResponse) {
                        is Response.Error -> {
                            submitSideEffect(CreateBadgeSideEffect.Notify.FailedToChangeState)
                        }
                        is Response.Success -> {}
                        is LoadResponse.Loading -> {}
                    }
                }
            }
        }
    }

    fun createBadge(
        badge: BadgeCreateDto
    ) {
        viewModelScope.launch(mainImmediate) {
            networkQueryPeople.createBadge(
                badge
            ).collect { loadResponse ->
                when (loadResponse) {
                    is Response.Error -> {
                        submitSideEffect(CreateBadgeSideEffect.Notify.FailedToCreateBadge)
                    }
                    is Response.Success -> {
                        updateViewState(CreateBadgeViewState.BadgeCreatedSuccessfully)
                    }
                    is LoadResponse.Loading -> {}
                }
            }
        }
    }



}
