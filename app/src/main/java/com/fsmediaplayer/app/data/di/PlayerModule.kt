package com.fsmediaplayer.app.data.di

import android.content.Context
import com.fsmediaplayer.app.core.player.FSPlayerManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

    @Provides
    @Singleton
    fun provideFSPlayerManager(
        @ApplicationContext context: Context
    ): FSPlayerManager {
        return FSPlayerManager(context)
    }
}
