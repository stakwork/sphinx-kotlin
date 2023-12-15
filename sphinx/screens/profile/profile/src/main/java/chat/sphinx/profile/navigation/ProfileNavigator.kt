package chat.sphinx.profile.navigation

import androidx.navigation.NavController
import io.matthewnelson.android_feature_navigation.requests.PopBackStack
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class ProfileNavigator(
    navigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(navigationDriver)
{
    abstract suspend fun toQRCodeDetail(qrText: String, viewTitle: String)

    abstract suspend fun toManageStorageDetail()


    suspend fun popBackStack() {
        navigationDriver.submitNavigationRequest(
                PopBackStack()
        )
    }

    abstract suspend fun toOnBoardWelcomeScreen()
}
