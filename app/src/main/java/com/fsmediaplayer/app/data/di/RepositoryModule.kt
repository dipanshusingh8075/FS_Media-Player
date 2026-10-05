package com.fsmediaplayer.app.data.di

import com.fsmediaplayer.app.data.repository.MediaStoreVideoRepositoryImpl
import com.fsmediaplayer.app.domain.repository.VideoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindVideoRepository(
        impl: MediaStoreVideoRepositoryImpl
    ): VideoRepository
}
