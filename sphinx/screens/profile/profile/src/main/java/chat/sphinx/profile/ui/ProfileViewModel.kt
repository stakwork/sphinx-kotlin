package chat.sphinx.profile.ui

import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import javax.inject.Inject

@HiltViewModel
internal class ProfileViewModel @Inject constructor(

): BaseViewModel<ProfileViewState>(ProfileViewState.Idle)
{
}