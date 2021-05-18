package chat.sphinx.profile.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.insetter_activity.addStatusBarPadding
import chat.sphinx.profile.R
import chat.sphinx.profile.databinding.FragmentProfileBinding
import chat.sphinx.profile.navigation.ProfileNavigator
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class ProfileFragment: BaseFragment<
        ProfileViewState,
        ProfileViewModel,
        FragmentProfileBinding
        >(R.layout.fragment_profile)
{
    override val viewModel: ProfileViewModel by viewModels()
    override val binding: FragmentProfileBinding by viewBinding(FragmentProfileBinding::bind)

    private val header: ConstraintLayout
        get() = binding.layoutProfileHeader.layoutConstraintProfileHeader
    private val headerNavBack: TextView
        get() = binding.layoutProfileHeader.textViewProfileHeaderNavBack

    @Inject
    protected lateinit var profileNavigator: ProfileNavigator

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        headerNavBack.setOnClickListener {
            lifecycleScope.launch {
                profileNavigator.popBackStack()
            }
        }

        setupProfileHeader()
    }

    private fun setupProfileHeader() {
        val activity = (requireActivity() as InsetterActivity)
        activity.addStatusBarPadding(header)
        activity.addNavigationBarPadding(binding.layoutConstraintProfile)

        header.layoutParams.height = header.layoutParams.height + activity.statusBarInsetHeight.top
        header.requestLayout()
    }

    override suspend fun onViewStateFlowCollect(viewState: ProfileViewState) {
//        TODO("Not yet implemented")
    }
}