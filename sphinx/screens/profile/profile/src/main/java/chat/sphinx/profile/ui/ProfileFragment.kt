package chat.sphinx.profile.ui

import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.profile.R
import chat.sphinx.profile.databinding.FragmentProfileBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment

@AndroidEntryPoint
internal class ProfileFragment: BaseFragment<
        ProfileViewState,
        ProfileViewModel,
        FragmentProfileBinding
        >(R.layout.fragment_profile)
{
    override val viewModel: ProfileViewModel by viewModels()
    override val binding: FragmentProfileBinding by viewBinding(FragmentProfileBinding::bind)

    override suspend fun onViewStateFlowCollect(viewState: ProfileViewState) {
//        TODO("Not yet implemented")
    }
}