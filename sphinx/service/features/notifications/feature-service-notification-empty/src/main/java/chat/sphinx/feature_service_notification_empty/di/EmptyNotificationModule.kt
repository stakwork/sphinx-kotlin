package chat.sphinx.feature_service_notification_empty.di

import chat.sphinx.concept_service_notification.PushNotificationRegistrar
import chat.sphinx.feature_service_notification_empty.EmptyPushNotificationRegistrar
import chat.sphinx.logger.SphinxLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object EmptyNotificationModule {

    @Provides
    @Singleton
    fun provideEmptyPushNotificationRegistrar(
        LOG: SphinxLogger,
    ): EmptyPushNotificationRegistrar =
        EmptyPushNotificationRegistrar(LOG)

    @Provides
    fun providePushNotificationRegistrar(
        emptyNotificationServiceControllerImpl: EmptyPushNotificationRegistrar
    ): PushNotificationRegistrar =
        emptyNotificationServiceControllerImpl
}
