package chat.sphinx.create_tribe.ui

import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import javax.inject.Inject

@HiltViewModel
internal class CreateTribeViewModel @Inject constructor(

): BaseViewModel<CreateTribeViewState>(CreateTribeViewState.Idle)
{
}
