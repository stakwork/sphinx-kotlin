package chat.sphinx.onboard_picture.ui

import android.content.Context
import chat.sphinx.concept_repository_contact.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import chat.sphinx.onboard_common.OnBoardStepHandler
import chat.sphinx.onboard_picture.navigation.OnBoardPictureNavigator
import javax.inject.Inject

@HiltViewModel
internal class OnBoardPictureViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    private val navigator: OnBoardPictureNavigator,
    private val contactRepository: ContactRepository,
    private val onBoardStepHandler: OnBoardStepHandler,
): SideEffectViewModel<
        Context,
        OnBoardPictureSideEffect,
        OnBoardPictureViewState
        >(dispatchers, OnBoardPictureViewState.Idle)
{

}
