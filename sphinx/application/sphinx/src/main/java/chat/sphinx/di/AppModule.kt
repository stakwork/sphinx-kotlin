package chat.sphinx.di

import chat.sphinx.BuildConfig
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.util.SphinxDispatchers
import chat.sphinx.util.SphinxLoggerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.matthewnelson.build_config.BuildConfigDebug
import io.matthewnelson.build_config.BuildConfigVersionCode
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun provideCoroutineDispatchers(
        sphinxDispatchers: SphinxDispatchers
    ): CoroutineDispatchers =
        sphinxDispatchers

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
}
