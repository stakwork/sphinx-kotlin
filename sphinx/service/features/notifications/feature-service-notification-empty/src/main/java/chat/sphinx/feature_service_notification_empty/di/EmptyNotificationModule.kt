package chat.sphinx.feature_service_notification_empty.di

import chat.sphinx.concept_service_notification.NotificationServiceController
import chat.sphinx.feature_service_notification_empty.EmptyNotificationServiceControllerImpl
import chat.sphinx.logger.SphinxLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EmptyNotificationModule {

    @Provides
    @Singleton
    fun provideEmptyNotificationServiceControllerImpl(
        LOG: SphinxLogger,
    ): EmptyNotificationServiceControllerImpl =
        EmptyNotificationServiceControllerImpl(LOG)

    @Provides
    fun provideNotificationServiceController(
        emptyNotificationServiceControllerImpl: EmptyNotificationServiceControllerImpl
    ): NotificationServiceController =
        emptyNotificationServiceControllerImpl
}
