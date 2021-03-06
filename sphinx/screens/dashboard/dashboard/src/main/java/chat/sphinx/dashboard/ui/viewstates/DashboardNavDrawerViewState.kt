package chat.sphinx.dashboard.ui.viewstates

import io.matthewnelson.android_concept_views.MotionLayoutViewState

internal sealed class DashboardNavDrawerViewState: MotionLayoutViewState<DashboardNavDrawerViewState>() {
    object Closed: DashboardNavDrawerViewState() {
        override val startSetId: Int
            get() = 0
        override val endSetId: Int?
            get() = null

    }
    object Open: DashboardNavDrawerViewState() {
        override val startSetId: Int
            get() = 0
        override val endSetId: Int?
            get() = null
    }
}