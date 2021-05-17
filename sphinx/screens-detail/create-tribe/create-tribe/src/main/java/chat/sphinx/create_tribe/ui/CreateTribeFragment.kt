package chat.sphinx.create_tribe.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.create_tribe.R
import chat.sphinx.create_tribe.databinding.FragmentCreateTribeBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment

@AndroidEntryPoint
internal class CreateTribeFragment: BaseFragment<
        CreateTribeViewState,
        CreateTribeViewModel,
        FragmentCreateTribeBinding
        >(R.layout.fragment_create_tribe)
{
    override val viewModel: CreateTribeViewModel by viewModels()
    override val binding: FragmentCreateTribeBinding by viewBinding(FragmentCreateTribeBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.includeCreateTribeHeader.apply {
            textViewDetailScreenHeaderName.text = getString(R.string.create_tribe_header_name)
//            textViewDetailScreenClose.setOnClickListener {
                // TODO: Navigate
//            }
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: CreateTribeViewState) {
//        TODO("Not yet implemented")
    }
}
