package chat.sphinx.contact_detail.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.contact_detail.R
import chat.sphinx.contact_detail.databinding.FragmentContactDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment
import kotlinx.coroutines.launch

@AndroidEntryPoint
internal class ContactDetailFragment: BaseFragment<
        ContactDetailViewState,
        ContactDetailViewModel,
        FragmentContactDetailBinding
        >(R.layout.fragment_contact_detail)
{
    override val viewModel: ContactDetailViewModel by viewModels()
    override val binding: FragmentContactDetailBinding by viewBinding(FragmentContactDetailBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.includeContactDetailHeader.apply {
            textViewDetailScreenHeaderName.text = getString(R.string.contact_detail_header_name)
            textViewDetailScreenClose.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.closeDetailScreen()
                }
            }
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: ContactDetailViewState) {
//        TODO("Not yet implemented")
    }
}
