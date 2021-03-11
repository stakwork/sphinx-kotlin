package chat.sphinx.activitymain.navigation.drivers

import androidx.navigation.NavController
import dagger.hilt.android.scopes.ActivityRetainedScoped
import io.matthewnelson.concept_navigation.NavigationRequest
import io.matthewnelson.feature_navigation.NavigationDriver
import javax.inject.Inject

@ActivityRetainedScoped
class PrimaryNavigationDriver @Inject constructor(

): NavigationDriver<NavController>(replayCacheSize = 5)
{
    override suspend fun whenTrueExecuteRequest(request: NavigationRequest<NavController>): Boolean {
        return true
    }
}
