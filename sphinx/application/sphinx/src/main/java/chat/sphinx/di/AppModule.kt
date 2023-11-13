package chat.sphinx.di

import android.app.Application
import android.content.Context
import android.widget.ImageView
import chat.sphinx.BuildConfig
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_network_client_cache.NetworkClientCache
import chat.sphinx.concept_wallet.WalletDataHandler
import chat.sphinx.example.concept_connect_manager.ConnectManager
import chat.sphinx.feature_connect_manager.ConnectManagerImpl
import chat.sphinx.feature_image_loader_android.ImageLoaderAndroid
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.notification.SphinxNotificationManager
import chat.sphinx.notification.SphinxNotificationManagerImpl
import chat.sphinx.util.SphinxDispatchers
import chat.sphinx.util.SphinxLoggerImpl
import chat.sphinx.wrapper_lightning.WalletMnemonic
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.matthewnelson.build_config.BuildConfigDebug
import io.matthewnelson.build_config.BuildConfigVersionCode
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_media_cache.MediaCacheHandler
import io.matthewnelson.feature_media_cache.MediaCacheHandlerImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSphinxDispatchers(): SphinxDispatchers =
        SphinxDispatchers()

    @Provides
    fun provideCoroutineDispatchers(
        sphinxDispatchers: SphinxDispatchers
    ): CoroutineDispatchers =
        sphinxDispatchers

    @Provides
    @Singleton
    fun provideApplicationScope(
        dispatchers: CoroutineDispatchers
    ): CoroutineScope =
        CoroutineScope(SupervisorJob() + dispatchers.default)

    @Provides
    @Singleton
    fun provideSphinxLoggerImpl(
        buildConfigDebug: BuildConfigDebug,
    ): SphinxLoggerImpl =
        SphinxLoggerImpl(buildConfigDebug)

    @Provides
    fun provideSphinxLogger(
        sphinxLoggerImpl: SphinxLoggerImpl
    ): SphinxLogger =
        sphinxLoggerImpl

    @Provides
    @Singleton
    fun provideBuildConfigDebug(): BuildConfigDebug =
        BuildConfigDebug(BuildConfig.DEBUG)

    @Provides
    @Singleton
    fun provideBuildConfigVersionCode(): BuildConfigVersionCode =
        BuildConfigVersionCode(BuildConfig.VERSION_CODE)

    @Provides
    @Singleton
    fun provideMoshi(): Moshi =
        Moshi.Builder().build()

    @Provides
    @Singleton
    fun provideImageLoaderAndroid(
        @ApplicationContext appContext: Context,
        dispatchers: CoroutineDispatchers,
        networkClientCache: NetworkClientCache,
        LOG: SphinxLogger,
    ): ImageLoaderAndroid =
        ImageLoaderAndroid(appContext, dispatchers, networkClientCache, LOG)

    @Provides
    fun provideImageLoader(
        imageLoaderAndroid: ImageLoaderAndroid
    ): ImageLoader<ImageView> =
        imageLoaderAndroid

    @Provides
    @Singleton
    fun provideMediaCacheHandler(
        applicationScope: CoroutineScope,
        application: Application,
        dispatchers: CoroutineDispatchers,
    ): MediaCacheHandler =
        MediaCacheHandlerImpl(
            applicationScope,
            application.cacheDir,
            dispatchers,
        )

    @Provides
    @Singleton
    fun provideSphinxNotificationManager(
        @ApplicationContext appContext: Context,
        sphinxLogger: SphinxLogger,
    ): SphinxNotificationManager = SphinxNotificationManagerImpl(
        appContext,
        sphinxLogger
    )

    @Provides
    @Singleton
    fun provideConnectManagerImpl(
        walletDataHandler: WalletDataHandler
    ): ConnectManagerImpl =
        ConnectManagerImpl(
            walletDataHandler
        )

    @Provides
    @Singleton
    fun provideConnectManager(
        connectManagerImpl: ConnectManagerImpl
    ): ConnectManager =
        connectManagerImpl

}
