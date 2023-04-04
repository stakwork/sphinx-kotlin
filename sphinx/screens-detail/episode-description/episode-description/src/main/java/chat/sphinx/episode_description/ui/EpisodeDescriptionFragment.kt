package chat.sphinx.episode_description.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_connectivity_helper.ConnectivityHelper
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.create_description.R
import chat.sphinx.create_description.databinding.FragmentEpisodeDescriptionBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
internal class EpisodeDescriptionFragment: SideEffectFragment<
        Context,
        EpisodeDescriptionSideEffect,
        EpisodeDescriptionViewState,
        EpisodeDescriptionViewModel,
        FragmentEpisodeDescriptionBinding
        >(R.layout.fragment_episode_description)
{
    override val viewModel: EpisodeDescriptionViewModel by viewModels()
    override val binding: FragmentEpisodeDescriptionBinding by viewBinding(FragmentEpisodeDescriptionBinding::bind)

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var connectivityHelper: ConnectivityHelper


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        BackPressHandler(viewLifecycleOwner, requireActivity())
    }

    private inner class BackPressHandler(
        owner: LifecycleOwner,
        activity: FragmentActivity,
    ) : OnBackPressedCallback(true) {
        init {
            activity.apply {
                onBackPressedDispatcher.addCallback(
                    owner,
                    this@BackPressHandler,
                )
            }
        }
        override fun handleOnBackPressed() {
            lifecycleScope.launch(viewModel.mainImmediate) {
                viewModel.navigator.popBackStack()
            }
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: EpisodeDescriptionViewState) {
    }

    override suspend fun onSideEffectCollect(sideEffect: EpisodeDescriptionSideEffect) {
        sideEffect.execute(binding.root.context)
    }

}
