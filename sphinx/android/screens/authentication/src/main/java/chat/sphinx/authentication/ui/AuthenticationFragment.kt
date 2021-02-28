package chat.sphinx.authentication.ui

import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.authentication.R
import chat.sphinx.authentication.databinding.FragmentAuthenticationBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.feature_authentication_view.ui.AuthenticationViewState

@AndroidEntryPoint
internal class AuthenticationFragment: SideEffectFragment<
        FragmentActivity,
        AuthenticationSideEffect,
        AuthenticationViewState,
        AuthenticationViewModel,
        FragmentAuthenticationBinding
        >(R.layout.fragment_authentication)
{
    override val viewModel: AuthenticationViewModel by viewModels()
    override val binding: FragmentAuthenticationBinding by viewBinding(FragmentAuthenticationBinding::bind)


    override suspend fun onSideEffectCollect(sideEffect: AuthenticationSideEffect) {
        // TODO("Not yet implemented")
    }
    override suspend fun onViewStateFlowCollect(viewState: AuthenticationViewState) {
        // TODO("Not yet implemented")
    }
}