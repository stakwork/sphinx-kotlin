package chat.sphinx.activitymain

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import androidx.lifecycle.viewModelScope
import app.cash.exhaustive.Exhaustive
import chat.sphinx.activitymain.navigation.drivers.AuthenticationNavigationDriver
import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.activitymain.ui.MainViewState
import chat.sphinx.concept_repository_actions.ActionsRepository
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.concept_service_media.MediaPlayerServiceController
import chat.sphinx.dashboard.navigation.ToDashboardScreen
import chat.sphinx.dashboard.ui.getMediaDuration
import chat.sphinx.wrapper_common.StorageData
import chat.sphinx.wrapper_common.StorageLimit.DEFAULT_STORAGE_LIMIT
import chat.sphinx.wrapper_common.StorageLimit.STORAGE_LIMIT_KEY
import chat.sphinx.wrapper_common.calculateUserStorageLimit
import chat.sphinx.wrapper_common.chat.PushNotificationLink
import chat.sphinx.wrapper_common.toFileSize
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_activity.NavigationViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_authentication.coordinator.AuthenticationCoordinator
import io.matthewnelson.concept_authentication.coordinator.AuthenticationRequest
import io.matthewnelson.concept_authentication.state.AuthenticationState
import io.matthewnelson.concept_authentication.state.AuthenticationStateManager
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authenticationCoordinator: AuthenticationCoordinator,
    private val authenticationStateManager: AuthenticationStateManager,
    val authenticationDriver: AuthenticationNavigationDriver,
    val detailDriver: DetailNavigationDriver,
    dispatchers: CoroutineDispatchers,
    private val app: Application,
    override val navigationDriver: PrimaryNavigationDriver,
    private val actionsRepository: ActionsRepository,
    private val feedRepository: FeedRepository,
    private val repositoryMedia: RepositoryMedia,
    private val mediaPlayerServiceController: MediaPlayerServiceController,
): BaseViewModel<MainViewState>(dispatchers, MainViewState.DetailScreenInactive), NavigationViewModel<PrimaryNavigationDriver>
{
    private var storageData: StorageData? = null
    private val storageLimitSharedPreferences: SharedPreferences = app.applicationContext.getSharedPreferences(STORAGE_LIMIT_KEY, Context.MODE_PRIVATE)

    init {
        viewModelScope.launch(mainImmediate) {
            authenticationStateManager.authenticationStateFlow.collect { state ->
                @Exhaustive
                when (state) {
                    is AuthenticationState.NotRequired -> {
                        // Do nothing
                    }
                    is AuthenticationState.Required.InitialLogIn -> {
                        // Handled by the Splash Screen
                    }
                    is AuthenticationState.Required.LoggedOut -> {
                        // Blow it up
                        authenticationCoordinator.submitAuthenticationRequest(
                            AuthenticationRequest.LogIn(privateKey = null)
                        )
                    }
                }
            }
        }
        getStorageData()
    }

    suspend fun handleDeepLink(deepLink: String) {
        if (authenticationStateManager.authenticationStateFlow.value == AuthenticationState.NotRequired) {
            navigationDriver.submitNavigationRequest(
                ToDashboardScreen(
                    popUpToId = R.id.main_primary_nav_graph,
                    updateBackgroundLoginTime = false,
                    deepLink = deepLink
                )
            )
        }
    }

    suspend fun handlePushNotification(chatId: Long) {
        val pushNotificationLink = PushNotificationLink("sphinx.chat://?action=push&chatId=$chatId")

        if (authenticationStateManager.authenticationStateFlow.value == AuthenticationState.NotRequired) {
            navigationDriver.submitNavigationRequest(
                ToDashboardScreen(
                    popUpToId = R.id.main_primary_nav_graph,
                    updateBackgroundLoginTime = false,
                    deepLink = pushNotificationLink.value
                )
            )
        }
    }

    fun syncActions() {
        actionsRepository.syncActions()
    }

    fun restoreContentFeedStatuses() {
        val playingContent = mediaPlayerServiceController.getPlayingContent()

        feedRepository.restoreContentFeedStatuses(
            playingContent?.first,
            playingContent?.second,
            ::retrieveEpisodeDuration
        )
    }

    fun saveContentFeedStatuses() {
        feedRepository.saveContentFeedStatuses()
    }

    private fun retrieveEpisodeDuration(
        url: String
    ) : Long {
        return Uri.parse(url).getMediaDuration(false)
    }

    fun getDeleteExcessFileIfApplicable(){
        viewModelScope.launch(mainImmediate) {
            storageData?.let { nnStorageData ->
                val storageLimitProgress = storageLimitSharedPreferences.getInt(
                    STORAGE_LIMIT_KEY,
                    DEFAULT_STORAGE_LIMIT
                )
                val userLimit = nnStorageData.freeStorage?.value?.let { calculateUserStorageLimit(freeStorage = it, seekBarValue = storageLimitProgress ) } ?: 0L
                val usageStorage = nnStorageData.usedStorage.value
                val excessSize = (usageStorage - userLimit)
                repositoryMedia.deleteExcessFilesOnBackground(excessSize)
            }
        }
    }

    private fun getStorageData(){
        viewModelScope.launch(mainImmediate) {
            repositoryMedia.getStorageDataInfo().collect { storageDataInfo ->
                val totalStorage = getTotalStorage()
                val usedStorage = storageDataInfo.usedStorage
                val freeStorage = (totalStorage - usedStorage.value).toFileSize()
                val modifiedStorageDataInfo = storageDataInfo.copy(freeStorage = freeStorage)
                storageData = modifiedStorageDataInfo
            }
        }
    }

    private fun getTotalStorage(): Long {
        val stat = StatFs(Environment.getDataDirectory().path)
        return stat.blockSizeLong * stat.availableBlocksLong
    }

}
