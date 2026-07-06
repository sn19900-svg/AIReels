package com.nabil.aireels.di

import com.nabil.aireels.data.repository.FfmpegVideoRepositoryImpl
import com.nabil.aireels.data.repository.GeminiRepositoryImpl
import com.nabil.aireels.data.repository.WhisperTranscriptionRepositoryImpl
import com.nabil.aireels.domain.repository.GeminiRepository
import com.nabil.aireels.domain.repository.TranscriptionRepository
import com.nabil.aireels.domain.repository.VideoRepository
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
    abstract fun bindGeminiRepository(impl: GeminiRepositoryImpl): GeminiRepository

    @Binds
    @Singleton
    abstract fun bindVideoRepository(impl: FfmpegVideoRepositoryImpl): VideoRepository

    @Binds
    @Singleton
    abstract fun bindTranscriptionRepository(impl: WhisperTranscriptionRepositoryImpl): TranscriptionRepository
}
