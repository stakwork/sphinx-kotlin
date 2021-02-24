package chat.sphinx.splash.ui

import androidx.lifecycle.ViewModel
import chat.sphinx.splash.navigation.SplashNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
internal class SplashViewModel @Inject constructor(
    private val navigator: SplashNavigator
): ViewModel() {


}