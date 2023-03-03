package chat.sphinx.create_badge.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.create_badge.R
import chat.sphinx.create_badge.navigation.CreateBadgeNavigator
import chat.sphinx.wrapper_badge.Badge
import chat.sphinx.wrapper_badge.BadgeTemplate
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
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
            this.argImage
        )
    }

@HiltViewModel
internal class CreateBadgeViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
    val navigator: CreateBadgeNavigator
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


}
