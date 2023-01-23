package chat.sphinx.activitymain.navigation.navigators.detail

import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.discover_tribes.navigation.DiscoverTribesNavigator
import chat.sphinx.join_tribe.navigation.ToJoinTribeDetail
import chat.sphinx.wrapper_common.tribe.TribeJoinLink
import javax.inject.Inject

internal class DiscoverTribesNavigatorImpl @Inject constructor(
    private val detailDriver: DetailNavigationDriver,
): DiscoverTribesNavigator(detailDriver) {

    override suspend fun toJoinTribeDetail(tribeLink: TribeJoinLink) {
        detailDriver.submitNavigationRequest(ToJoinTribeDetail(tribeLink))
    }

}
