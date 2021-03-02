package chat.sphinx.activitymain

import androidx.lifecycle.ViewModel
import chat.sphinx.activitymain.navigation.MainNavigationDriver
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_activity.NavigationViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    override val navigationDriver: MainNavigationDriver
): ViewModel(), NavigationViewModel<MainNavigationDriver>
{

}
