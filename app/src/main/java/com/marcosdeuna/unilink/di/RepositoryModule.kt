package com.marcosdeuna.unilink.di

import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.StorageReference
import com.google.gson.Gson
import com.marcosdeuna.unilink.data.repository.AuthRepository
import com.marcosdeuna.unilink.data.repository.AuthRepositoryImpl
import com.marcosdeuna.unilink.data.repository.PostRepository
import com.marcosdeuna.unilink.data.repository.PostRepositoryImpl
import com.marcosdeuna.unilink.data.repository.UserRepository
import com.marcosdeuna.unilink.data.repository.UserRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object RepositoryModule {
    @Provides
    @Singleton
    fun providePostRepository(database: FirebaseFirestore, storageReference: StorageReference): PostRepository {
        return PostRepositoryImpl(database, storageReference)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(database: FirebaseFirestore, storageReference: StorageReference, auth: FirebaseAuth, appPreferences: SharedPreferences, gson: Gson): AuthRepository {
        return AuthRepositoryImpl(database, storageReference,  auth, appPreferences, gson)
    }

    @Provides
    @Singleton
    fun provideUserRepository(database: FirebaseFirestore, appPreferences: SharedPreferences, gson: Gson): UserRepository {
        return UserRepositoryImpl(database, appPreferences, gson)
    }
}