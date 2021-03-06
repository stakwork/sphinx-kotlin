package chat.sphinx.add_sats.ui

import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class AddSatsViewModel @Inject constructor(

): BaseViewModel<AddSatsViewState>(AddSatsViewState.Idle)
{
}