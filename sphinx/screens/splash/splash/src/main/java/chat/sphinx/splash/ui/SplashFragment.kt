package chat.sphinx.splash.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.insetter_activity.addStatusBarPadding
import chat.sphinx.splash.R
import chat.sphinx.splash.databinding.FragmentSplashBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment

@AndroidEntryPoint
internal class SplashFragment: BaseFragment<
        SplashViewState,
        SplashViewModel,
        FragmentSplashBinding
        >(R.layout.fragment_splash)
{

    override val binding: FragmentSplashBinding by viewBinding(FragmentSplashBinding::bind)
    override val viewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.screenInit()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHeaderAndFooter()
    }

    private fun setupHeaderAndFooter() {
        (requireActivity() as InsetterActivity)
            .addStatusBarPadding(binding.layoutConstraintSplash)
            .addNavigationBarPadding(binding.layoutConstraintSplash)
    }

    override suspend fun onViewStateFlowCollect(viewState: SplashViewState) {
        //TODO implement view state collector
    }
}
