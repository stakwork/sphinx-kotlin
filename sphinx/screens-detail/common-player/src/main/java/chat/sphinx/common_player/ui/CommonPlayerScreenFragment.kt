package chat.sphinx.common_player.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.common_player.R
import chat.sphinx.common_player.adapter.RecommendedItemsAdapter
import chat.sphinx.common_player.databinding.FragmentCommonPlayerScreenBinding
import chat.sphinx.common_player.viewstate.CommonPlayerScreenViewState
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.insetter_activity.InsetterActivity
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import javax.inject.Inject

@AndroidEntryPoint
internal class CommonPlayerScreenFragment(
) : SideEffectFragment<
        FragmentActivity,
        CommonPlayerScreenSideEffect,
        CommonPlayerScreenViewState,
        CommonPlayerScreenViewModel,
        FragmentCommonPlayerScreenBinding
        >(R.layout.fragment_common_player_screen) {

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    override val binding: FragmentCommonPlayerScreenBinding by viewBinding(
        FragmentCommonPlayerScreenBinding::bind
    )
    override val viewModel: CommonPlayerScreenViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupItems()
    }

    override suspend fun onViewStateFlowCollect(viewState: CommonPlayerScreenViewState) {
    }

    override suspend fun onSideEffectCollect(sideEffect: CommonPlayerScreenSideEffect) {
    }

    private fun setupItems() {
        binding.includeRecommendedItemsList.let {
            it.recyclerViewVideoList.apply {
                val linearLayoutManager = LinearLayoutManager(context)
                val recommendedItemsAdapter = RecommendedItemsAdapter(
                    imageLoader,
                    viewLifecycleOwner,
                    onStopSupervisor,
                    viewModel,
                    viewModel
                )
//                val recommendedListFooterAdapter =
//                    RecommendedItemsFooterAdapter(requireActivity() as InsetterActivity)
//                this.setHasFixedSize(false)
                layoutManager = linearLayoutManager
                adapter = recommendedItemsAdapter
                itemAnimator = null
            }
        }
    }

}