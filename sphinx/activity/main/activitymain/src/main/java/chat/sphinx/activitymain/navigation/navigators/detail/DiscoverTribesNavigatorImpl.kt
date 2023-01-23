package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.R
import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.chat_tribe.navigation.ToChatTribeScreen
import chat.sphinx.discover_tribes.navigation.DiscoverTribesNavigator
import chat.sphinx.join_tribe.navigation.ToJoinTribeDetail
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.tribe.TribeJoinLink
import javax.inject.Inject

internal class DiscoverTribesNavigatorImpl @Inject constructor(
    private val detailDriver: DetailNavigationDriver,
): DiscoverTribesNavigator(detailDriver) {

    override suspend fun toJoinTribeDetail(tribeLink: TribeJoinLink) {
        detailDriver.submitNavigationRequest(ToJoinTribeDetail(tribeLink))
    }

    override suspend fun toChatTribe(chatId: ChatId) {
        detailDriver.submitNavigationRequest(
            ToChatTribeScreen(
                chatId = chatId,
                popUpToId = R.id.navigation_dashboard_fragment,
                popUpToInclusive = false,
            )
        )
    }

}
