package chat.sphinx.create_badge.ui

import android.content.Context
import chat.sphinx.create_badge.navigation.CreateBadgeNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject

@HiltViewModel
internal class CreateBadgeViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    val navigator: CreateBadgeNavigator
): SideEffectViewModel<
        Context,
        CreateBadgeSideEffect,
        CreateBadgeViewState>(dispatchers, CreateBadgeViewState.Idle)
{


}
