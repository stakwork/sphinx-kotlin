package chat.sphinx.discover_tribes.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.discover_tribes.R
import chat.sphinx.discover_tribes.databinding.FragmentDiscoverTribesBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment

@AndroidEntryPoint
internal class DiscoverTribesFragment: BaseFragment<
        DiscoverTribesViewState,
        DiscoverTribesViewModel,
        FragmentDiscoverTribesBinding
        >(R.layout.fragment_discover_tribes) {

    override val viewModel: DiscoverTribesViewModel by viewModels()
    override val binding: FragmentDiscoverTribesBinding by viewBinding(FragmentDiscoverTribesBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
    }

    override suspend fun onViewStateFlowCollect(viewState: DiscoverTribesViewState) {
    }

    override fun subscribeToViewStateFlow() {
        super.subscribeToViewStateFlow()
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}