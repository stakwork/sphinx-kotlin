package chat.sphinx.di

import chat.sphinx.BuildConfig
import chat.sphinx.util.SphinxDispatchers
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
    @Singleton
    fun provideBuildConfigDebug(): BuildConfigDebug =
        BuildConfigDebug(BuildConfig.DEBUG)

    @Provides
    @Singleton
    fun provideBuildConfigVersionCode(): BuildConfigVersionCode =
        BuildConfigVersionCode(BuildConfig.VERSION_CODE)
}
