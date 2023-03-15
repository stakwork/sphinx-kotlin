package chat.sphinx.tribe_badge.navigation

import androidx.navigation.NavController
import chat.sphinx.create_badge.navigation.ToCreateBadge
import io.matthewnelson.android_feature_navigation.requests.PopBackStack
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class TribeBadgesNavigator(
    navigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(navigationDriver) {
    abstract suspend fun closeDetailScreen()

    @JvmSynthetic
    internal suspend fun popBackStack() {
        navigationDriver.submitNavigationRequest(PopBackStack())
    }

    @JvmSynthetic
    suspend fun toCreateBadgeScreen(
        name: String,
        description: String,
        image: String,
        rewardType: Int,
        rewardRequirement: Int,
        isActive: Boolean,
        chatId: Int,
        badgeId: Int,
        amountCreated: Int,
        amountIssued: Int,
        claimAmount: Int,
        holderType: Int
    ) {
        navigationDriver.submitNavigationRequest(
            ToCreateBadge(
                name,
                description,
                image,
                rewardType,
                rewardRequirement,
                isActive,
                chatId,
                badgeId,
                amountCreated,
                amountIssued,
                claimAmount,
                holderType
            )
        )
    }
}
