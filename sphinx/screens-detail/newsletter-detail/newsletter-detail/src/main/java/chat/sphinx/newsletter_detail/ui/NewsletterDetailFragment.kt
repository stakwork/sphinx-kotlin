package chat.sphinx.newsletter_detail.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.newsletter_detail.R
import chat.sphinx.newsletter_detail.databinding.FragmentNewsletterDetailBinding
import chat.sphinx.newsletter_detail.ui.adapter.NewsletterItemsFooterAdapter
import chat.sphinx.newsletter_detail.ui.adapter.NewsletterItemsListAdapter
import chat.sphinx.screen_detail_fragment.BaseDetailFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

@AndroidEntryPoint
internal class NewsletterDetailFragment: BaseDetailFragment<
        NewsletterDetailViewState,
        NewsletterDetailViewModel,
        FragmentNewsletterDetailBinding
        >(R.layout.fragment_newsletter_detail)
{
    override val viewModel: NewsletterDetailViewModel by viewModels()
    override val binding: FragmentNewsletterDetailBinding by viewBinding(FragmentNewsletterDetailBinding::bind)

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            textViewDetailScreenClose.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.closeDetailScreen()
                }
            }
        }

        setupItems()
    }

    override fun closeDetailsScreen() {
        lifecycleScope.launch(viewModel.mainImmediate) {
            viewModel.navigator.closeDetailScreen()
        }
    }

    private fun setupItems() {
        binding.recyclerViewNewsletterItems.apply {
            val linearLayoutManager = LinearLayoutManager(context)
            val chatListAdapter = NewsletterItemsListAdapter(
                this,
                linearLayoutManager,
                imageLoader,
                viewLifecycleOwner,
                onStopSupervisor,
                viewModel
            )

            val episodesListFooterAdapter = NewsletterItemsFooterAdapter(requireActivity() as InsetterActivity)
            this.setHasFixedSize(false)
            layoutManager = linearLayoutManager
            adapter = ConcatAdapter(chatListAdapter, episodesListFooterAdapter)
            itemAnimator = null
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: NewsletterDetailViewState) {
        @Exhaustive
        when (viewState) {
            is NewsletterDetailViewState.Idle -> {}

            is NewsletterDetailViewState.FeedLoaded -> {
                binding.apply {
                    textViewNewsletterTitle.text = viewState.title.value
                    textViewNewsletterDescription.text = viewState.description?.value ?: ""

                    viewState.image?.value?.let { feedImage ->
                        imageLoader.load(
                            imageViewNewsletterImage,
                            feedImage,
                            ImageLoaderOptions.Builder()
                                .placeholderResId(R.drawable.ic_newsletter_placeholder)
                                .build()
                        )
                    }

                    textViewNewsletterItemsCount.text = viewState.items.count().toString()
                }
            }
        }
    }
}
