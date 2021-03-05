package chat.sphinx.home.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import chat.sphinx.home.R
import chat.sphinx.resources.SphinxToastUtils
import io.matthewnelson.android_feature_screens.navigation.CloseAppOnBackPress

class HomeFragment: Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        CloseAppOnBackPress(view.context)
            .enableDoubleTapToClose(
                viewLifecycleOwner,
                SphinxToastUtils()
            )
            .addCallback(
                viewLifecycleOwner,
                requireActivity()
            )
    }
}