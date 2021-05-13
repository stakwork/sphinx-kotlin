package chat.sphinx.feature_service_notification_firebase.di

import chat.sphinx.concept_service_notification.NotificationServiceController
import chat.sphinx.feature_service_notification_firebase.NotificationServiceControllerImpl
import chat.sphinx.logger.SphinxLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideNotificationServiceControllerImpl(
        LOG: SphinxLogger,
    ): NotificationServiceControllerImpl =
        NotificationServiceControllerImpl(LOG)

    @Provides
    fun provideNotificationServiceController(
        notificationServiceControllerImpl: NotificationServiceControllerImpl
    ): NotificationServiceController =
        notificationServiceControllerImpl
}
