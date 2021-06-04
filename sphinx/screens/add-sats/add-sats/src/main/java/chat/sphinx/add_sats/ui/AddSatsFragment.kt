package chat.sphinx.add_sats.ui

import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.add_sats.R
import chat.sphinx.add_sats.databinding.FragmentAddSatsBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment

@AndroidEntryPoint
internal class AddSatsFragment: BaseFragment<
        AddSatsViewState,
        AddSatsViewModel,
        FragmentAddSatsBinding
        >(R.layout.fragment_add_sats)
{
    override val viewModel: AddSatsViewModel by viewModels()
    override val binding: FragmentAddSatsBinding by viewBinding(FragmentAddSatsBinding::bind)

    override suspend fun onViewStateFlowCollect(viewState: AddSatsViewState) {
//        TODO("Not yet implemented")
    }
}
