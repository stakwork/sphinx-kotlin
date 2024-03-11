package chat.sphinx.tribes_discover.ui

import android.content.Context
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_view_model_coordinator.ResponseHolder
import chat.sphinx.feature_view_model_coordinator.RequestCatcher
import chat.sphinx.kotlin_response.Response
import chat.sphinx.tribes_discover.coordinator.TribesDiscoverViewModelCoordinator
import chat.sphinx.tribes_discover.model.DiscoverTribesTag
import chat.sphinx.tribes_discover.navigation.BackType
import chat.sphinx.tribes_discover.navigation.TribesDiscoverNavigator
import chat.sphinx.tribes_discover.viewstate.DiscoverTribesTagsViewState
import chat.sphinx.tribes_discover.viewstate.DiscoverTribesViewState
import chat.sphinx.tribes_discover.viewstate.TribeHolderViewState
import chat.sphinx.tribes_discover_view_model_coordinator.response.TribesDiscoverResponse
import chat.sphinx.wrapper_common.chat.ChatUUID
import chat.sphinx.wrapper_common.chat.toChatUUID
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class TribesDiscoverViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    val navigator: TribesDiscoverNavigator,
    private val chatRepository: ChatRepository,
    private val tribesDiscoverViewModelCoordinator: TribesDiscoverViewModelCoordinator
): SideEffectViewModel<
    Context,
    TribesDiscoverSideEffect,
    DiscoverTribesViewState,
    >(dispatchers, DiscoverTribesViewState.Loading)
{

    companion object {
        private const val TRIBES_DEFAULT_SERVER_URL = "34.229.52.200:8801"
    }

        val discoverTribesTagsViewStateContainer: ViewStateContainer<DiscoverTribesTagsViewState> by lazy {
        ViewStateContainer(DiscoverTribesTagsViewState.Closed(0))
    }

    var page = 1
    var itemsPerPage = 20

    private val _tribeTagsStateFlow: MutableStateFlow<List<DiscoverTribesTag>> by lazy {
        MutableStateFlow(
            listOf(
                DiscoverTribesTag("NSFW"),
                DiscoverTribesTag("Bitcoin"),
                DiscoverTribesTag("Lightning"),
                DiscoverTribesTag("Sphinx"),
                DiscoverTribesTag("Podcast"),
                DiscoverTribesTag("Crypto"),
                DiscoverTribesTag("Music"),
                DiscoverTribesTag("Tech"),
                DiscoverTribesTag("Altcoins")
            )
        )
    }


    val tribeTagsStateFlow: StateFlow<List<DiscoverTribesTag>>
        get() = _tribeTagsStateFlow.asStateFlow()

    fun toggleTagWith(index: Int) {
        val currentTag = _tribeTagsStateFlow.value[index]

        _tribeTagsStateFlow.value = _tribeTagsStateFlow.value.map {
            if (it.name == currentTag.name)
                DiscoverTribesTag(currentTag.name, !currentTag.isSelected)
            else it
        }
    }

    private val tribeSelectedTagsList: MutableList<String> = mutableListOf()

    private val joinedTribesUUIDs: MutableStateFlow<List<ChatUUID>?> by lazy {
        MutableStateFlow(null)
    }

    init {
        viewModelScope.launch(mainImmediate) {
            joinedTribesUUIDs.value = chatRepository.getAllTribeChats.firstOrNull()?.map { it.uuid }
        }
    }

    fun showTagsView() {
        discoverTribesTagsViewStateContainer.updateViewState(DiscoverTribesTagsViewState.Open)
    }

    fun resetTags() {
        viewModelScope.launch(mainImmediate) {
            discoverTribesTagsViewStateContainer.updateViewState(
                DiscoverTribesTagsViewState.Closed(
                    tribeSelectedTagsList.size
                )
            )

            delay(500L)

            _tribeTagsStateFlow.value = _tribeTagsStateFlow.value.map {
                if (tribeSelectedTagsList.contains(it.name))
                    DiscoverTribesTag(it.name, true)
                else DiscoverTribesTag(it.name, false)
            }
        }
    }

    fun applyTags(
        searchTerm: String? = null
    ) {
        tribeSelectedTagsList.clear()

        for (discoverTribesTag in tribeTagsStateFlow.value) {
            if (discoverTribesTag.isSelected && !tribeSelectedTagsList.contains(discoverTribesTag.name)) {
                tribeSelectedTagsList.add(discoverTribesTag.name)
            }
        }

        page = 1
        getDiscoverTribesList(searchTerm)

        discoverTribesTagsViewStateContainer.updateViewState(
            DiscoverTribesTagsViewState.Closed(
                tribeSelectedTagsList.size
            )
        )
    }

    fun loadNextPage(
        searchTerm: String? = null
    ) {
        page += 1

        getDiscoverTribesList(
            searchTerm,
            page
        )
    }

    private var discoverTribeJob: Job? = null
    fun getDiscoverTribesList(
        searchTerm: String? = null,
        page: Int = 1
    ) {
        if (discoverTribeJob?.isActive == true) {
            return
        }

        discoverTribeJob = viewModelScope.launch(mainImmediate) {

            if (page == 1) {
                updateViewState(DiscoverTribesViewState.Loading)
            }

            val viewStateTribes = (viewStateContainer.value as? DiscoverTribesViewState.Tribes)

            if (viewStateTribes?.isLastPage == true) {
                return@launch
            }

            val tags: String = tribeSelectedTagsList.joinToString(",")

            chatRepository.getAllDiscoverTribes(
                page,
                itemsPerPage,
                searchTerm,
                tags
            ).collect { discoverTribes ->

                val existingTribes = if (page > 1) {
                    (viewStateTribes?.tribes ?: listOf())
                } else listOf()

                val tribesHVSs = ArrayList<TribeHolderViewState>(discoverTribes.size)

                discoverTribes.forEach {
                    it.joined = (joinedTribesUUIDs.value as List<ChatUUID>).contains(it.uuid?.toChatUUID())

                    tribesHVSs.add(
                        TribeHolderViewState.Tribe(it)
                    )
                }

                if (discoverTribes.size == itemsPerPage) {
                    tribesHVSs.add(
                        TribeHolderViewState.Loader
                    )
                }

                val updatedTribes = existingTribes.dropLast(1) + tribesHVSs

                updateViewState(
                    DiscoverTribesViewState.Tribes(
                        updatedTribes,
                        discoverTribes.size < itemsPerPage
                    )
                )
            }
        }
    }

    private val requestCatcher = RequestCatcher(
        viewModelScope,
        tribesDiscoverViewModelCoordinator,
        mainImmediate
    )

    private var responseJob: Job? = null
    fun handleTribeLink(pubkey: String) {
        if (responseJob?.isActive == true) {
            return
        }

        responseJob = viewModelScope.launch(mainImmediate) {
            try {
                requestCatcher.getCaughtRequestStateFlow().collect { list ->
                    list.firstOrNull()?.let { requestHolder ->

                        tribesDiscoverViewModelCoordinator.submitResponse(
                            response = Response.Success(
                                ResponseHolder(
                                    requestHolder,
                                    TribesDiscoverResponse(
                                        "sphinx.chat://?action=tribeV2&pubkey=${pubkey}&host=${TRIBES_DEFAULT_SERVER_URL}"
                                    )
                                )
                            ),
                            navigateBack = BackType.CloseDetailScreen
                        )
                    }
                }
            } catch (e: Exception) {}
        }
    }

}