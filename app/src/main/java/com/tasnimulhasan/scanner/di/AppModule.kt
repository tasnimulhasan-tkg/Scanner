package com.tasnimulhasan.scanner.di

import com.tasnimulhasan.scanner.data.repository.OcrRepositoryImpl
import com.tasnimulhasan.scanner.domain.repository.OcrRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    /**
     * Binds the data-layer implementation to the domain interface.
     * This is the Dependency Inversion Principle in action.
     */
    @Binds
    @Singleton
    abstract fun bindOcrRepository(impl: OcrRepositoryImpl): OcrRepository
}