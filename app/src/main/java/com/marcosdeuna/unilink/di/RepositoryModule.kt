package com.marcosdeuna.unilink.di

import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.StorageReference
import com.google.gson.Gson
import com.marcosdeuna.unilink.data.repository.AuthRepository
import com.marcosdeuna.unilink.data.repository.AuthRepositoryImpl
import com.marcosdeuna.unilink.data.repository.NoteRepository
import com.marcosdeuna.unilink.data.repository.NoteRepositoryImpl
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
    fun provideNoteRepository(database: FirebaseFirestore, storageReference: StorageReference): NoteRepository {
        return NoteRepositoryImpl(database, storageReference)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(database: FirebaseFirestore, storageReference: StorageReference, auth: FirebaseAuth, appPreferences: SharedPreferences, gson: Gson): AuthRepository {
        return AuthRepositoryImpl(database, storageReference,  auth, appPreferences, gson)
    }
}