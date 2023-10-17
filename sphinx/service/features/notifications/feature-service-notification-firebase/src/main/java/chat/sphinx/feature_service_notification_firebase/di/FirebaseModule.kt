package chat.sphinx.feature_service_notification_firebase.di

import android.content.Context
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_service_notification.PushNotificationRegistrar
import chat.sphinx.feature_service_notification_firebase.FirebasePushNotificationRegistrar
import chat.sphinx.logger.SphinxLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebasePushNotificationRegistrar(
        contactRepository: ContactRepository,
        LOG: SphinxLogger,
        @ApplicationContext appContext: Context,
    ): FirebasePushNotificationRegistrar =
        FirebasePushNotificationRegistrar(
            contactRepository,
            LOG,
            appContext,
        )

    @Provides
    fun providePushNotificationRegistrar(
        notificationServiceControllerImpl: FirebasePushNotificationRegistrar
    ): PushNotificationRegistrar =
        notificationServiceControllerImpl
}
