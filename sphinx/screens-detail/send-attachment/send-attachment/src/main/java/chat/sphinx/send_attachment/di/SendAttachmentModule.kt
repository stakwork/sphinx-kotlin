package chat.sphinx.send_attachment.di

import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.send_attachment.coordinator.SendAttachmentViewModelCoordinator
import chat.sphinx.send_attachment.navigation.SendAttachmentNavigator
import chat.sphinx.send_attachment_view_model_coordinator.request.SendAttachmentRequest
import chat.sphinx.send_attachment_view_model_coordinator.response.SendAttachmentResponse
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped
import io.matthewnelson.concept_coroutines.CoroutineDispatchers


@Module
@InstallIn(ActivityRetainedComponent::class)
internal object SendAttachmentModule {

    @Provides
    @ActivityRetainedScoped
    fun provideScannerCoordinator(
        dispatchers: CoroutineDispatchers,
        sendAttachmentNavigator: SendAttachmentNavigator,
        LOG: SphinxLogger,
    ): SendAttachmentViewModelCoordinator =
        SendAttachmentViewModelCoordinator(dispatchers, sendAttachmentNavigator, LOG)

    @Provides
    fun provideViewModelCoordinator(
        sendAttachmentViewModelCoordinator: SendAttachmentViewModelCoordinator
    ): ViewModelCoordinator<SendAttachmentRequest, SendAttachmentResponse> =
        sendAttachmentViewModelCoordinator
}
