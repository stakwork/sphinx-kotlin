package chat.sphinx.create_badge.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.create_badge.R
import chat.sphinx.create_badge.navigation.CreateBadgeNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject

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
        updateViewState(
            CreateBadgeViewState.EditBadge(
                args.badgeName,
                args.badgeImage,
                args.badgeDescription,
                args.badgeAmount,
                args.badgeLeft,
                args.badgeActive
            )
        )
    }


}
