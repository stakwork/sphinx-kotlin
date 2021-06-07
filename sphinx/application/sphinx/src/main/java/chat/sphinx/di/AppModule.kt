package chat.sphinx.di

import android.content.Context
import android.widget.ImageView
import chat.sphinx.BuildConfig
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_network_client_cache.NetworkClientCache
import chat.sphinx.concept_user_colors.UserColors
import chat.sphinx.feature_image_loader_android.ImageLoaderAndroid
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.user_colors.UserColorsImpl
import chat.sphinx.util.SphinxDispatchers
import chat.sphinx.util.SphinxLoggerImpl
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.matthewnelson.build_config.BuildConfigDebug
import io.matthewnelson.build_config.BuildConfigVersionCode
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
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
        networkClientCache: NetworkClientCache
    ): ImageLoaderAndroid =
        ImageLoaderAndroid(appContext, dispatchers, networkClientCache)

    @Provides
    fun provideImageLoader(
        imageLoaderAndroid: ImageLoaderAndroid
    ): ImageLoader<ImageView> =
        imageLoaderAndroid

    @Provides
    @Singleton
    fun provideUserColorsImpl(
        @ApplicationContext appContext: Context,
        dispatchers: CoroutineDispatchers
    ): UserColorsImpl =
        UserColorsImpl(appContext, dispatchers)

    @Provides
    fun provideUserColors(
        userColorsImpl: UserColorsImpl
    ): UserColors =
        userColorsImpl
}
