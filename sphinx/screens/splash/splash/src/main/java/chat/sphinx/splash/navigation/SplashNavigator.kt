package chat.sphinx.splash.navigation

import androidx.navigation.NavController
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class SplashNavigator(
    navigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(navigationDriver)
{
    abstract suspend fun toScannerDetail()
    abstract suspend fun toDashboardScreen(privateMode: Boolean = false)
    abstract suspend fun toOnBoardScreen(input: String)
}
