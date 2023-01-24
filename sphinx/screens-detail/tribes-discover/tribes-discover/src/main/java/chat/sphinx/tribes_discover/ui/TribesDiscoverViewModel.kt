package chat.sphinx.tribes_discover.ui

import android.content.Context
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_network_query_chat.model.TribeDto
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_view_model_coordinator.ResponseHolder
import chat.sphinx.feature_view_model_coordinator.RequestCatcher
import chat.sphinx.kotlin_response.Response
import chat.sphinx.tribes_discover.coordinator.TribesDiscoverViewModelCoordinator
import chat.sphinx.tribes_discover.model.DiscoverTribesTag
import chat.sphinx.tribes_discover.navigation.BackType
import chat.sphinx.tribes_discover.navigation.TribesDiscoverNavigator
import chat.sphinx.tribes_discover.viewstate.DiscoverTribesLoadingViewState
import chat.sphinx.tribes_discover.viewstate.DiscoverTribesTagsViewState
import chat.sphinx.tribes_discover.viewstate.DiscoverTribesViewState
import chat.sphinx.tribes_discover_view_model_coordinator.response.TribesDiscoverResponse
import chat.sphinx.wrapper_common.chat.ChatUUID
import chat.sphinx.wrapper_common.chat.toChatUUID
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import kotlinx.coroutines.Job
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
    >(dispatchers, DiscoverTribesViewState.Idle)
{
    private val joinedTribesUUIDs: MutableStateFlow<List<ChatUUID>?> by lazy {
        MutableStateFlow(null)
    }

    init {
        viewModelScope.launch(mainImmediate) {
            joinedTribesUUIDs.value = chatRepository.getAllTribeChats.firstOrNull()?.map { it.uuid }
        }
    }

    val discoverTribesTagsViewStateContainer: ViewStateContainer<DiscoverTribesTagsViewState> by lazy {
        ViewStateContainer(DiscoverTribesTagsViewState.Closed)
    }

    val discoverTribesLoadingViewStateContainer: ViewStateContainer<DiscoverTribesLoadingViewState> by lazy {
        ViewStateContainer(DiscoverTribesLoadingViewState.Open)
    }

    var page = 1

    private val _tribeTagsStateFlow: MutableStateFlow<Array<DiscoverTribesTag>> by lazy {
        MutableStateFlow(
            arrayOf(
                DiscoverTribesTag("NSFW", false),
                DiscoverTribesTag("Bitcoin", false),
                DiscoverTribesTag("Lightning", false),
                DiscoverTribesTag("Sphinx", false),
                DiscoverTribesTag("Podcast", false),
                DiscoverTribesTag("Crypto", false),
                DiscoverTribesTag("Music", false),
                DiscoverTribesTag("Tech", false),
                DiscoverTribesTag("Altcoins", false)
            )
        )
    }

    val tribeTagsStateFlow: StateFlow<Array<DiscoverTribesTag>>
        get() = _tribeTagsStateFlow.asStateFlow()

    fun changeSelectTag(position: Int) {
        _tribeTagsStateFlow.value[position].isSelected = !_tribeTagsStateFlow.value[position].isSelected
    }

    private val _tribeSelectedTagsList: MutableStateFlow<List<String>?> by lazy {
        MutableStateFlow(null)
    }

    val tribeSelectedTagsList: StateFlow<List<String>?>
        get() = _tribeSelectedTagsList.asStateFlow()

    fun getTribeSelectedTags() {
        _tribeSelectedTagsList.value = tribeTagsStateFlow.value.filter { it.isSelected }.map { it.name }
    }

    private val _discoverTribeStateFlow: MutableStateFlow<List<TribeDto>> by lazy {
        MutableStateFlow(listOf())
    }

    val discoverTribesStateFlow: StateFlow<List<TribeDto>>
        get() = _discoverTribeStateFlow.asStateFlow()


    private var discoverTribeJob: Job? = null
    fun getDiscoverTribesList(
        searchTerm: String? = null
    ) {
        if (discoverTribeJob?.isActive == true) {
            return
        }

        discoverTribeJob = viewModelScope.launch(mainImmediate) {

            val tags: String? = tribeSelectedTagsList.value?.toString()
                ?.replace("[", "")
                ?.replace("]", "")
                ?.replace("\\s".toRegex(), "")

            chatRepository.getAllDiscoverTribes(
                page,
                searchTerm,
                tags
            ).collect { discoverTribes ->

                discoverTribes.forEach {
                    it.joined = (joinedTribesUUIDs.value as List<ChatUUID>)?.contains(it.uuid?.toChatUUID())
                }

                _discoverTribeStateFlow.value = _discoverTribeStateFlow.value + discoverTribes

                discoverTribesLoadingViewStateContainer.updateViewState(
                    DiscoverTribesLoadingViewState.Closed
                )
            }
        }
    }

    fun cleanDiscoverTribesList() {
        _discoverTribeStateFlow.value = listOf()
    }

    private val requestCatcher = RequestCatcher(
        viewModelScope,
        tribesDiscoverViewModelCoordinator,
        mainImmediate
    )

    private var responseJob: Job? = null
    fun handleTribeLink(uuid: String) {
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
                                        "sphinx.chat://?action=tribe&uuid=${uuid}&host=tribes.sphinx.chat"
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