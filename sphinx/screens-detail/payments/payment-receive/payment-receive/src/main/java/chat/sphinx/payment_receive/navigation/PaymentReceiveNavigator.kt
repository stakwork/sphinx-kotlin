package chat.sphinx.payment_receive.navigation

import androidx.navigation.NavController
import chat.sphinx.payment_common.navigation.PaymentNavigator
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class PaymentReceiveNavigator(
    navigationDriver: BaseNavigationDriver<NavController>
): PaymentNavigator(navigationDriver) {

}
