package chat.sphinx.example.manage_storage.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import chat.sphinx.detail_resources.DetailNavOptions
import chat.sphinx.manage.storage.R
import io.matthewnelson.concept_navigation.NavigationRequest
import io.matthewnelson.android_feature_navigation.R as nav_R

class ToManageStorageDetail(
    private val options: NavOptions = DetailNavOptions.defaultBuilt
): NavigationRequest<NavController>() {
    override fun navigate(controller: NavController) {
        controller.navigate(
            R.id.manage_storage_nav_graph,
            null,
            options
        )
    }

}
