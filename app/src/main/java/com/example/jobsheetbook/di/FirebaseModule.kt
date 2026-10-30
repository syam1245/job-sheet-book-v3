package com.example.jobsheetbook.di

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        val firestore = Firebase.firestore
        val settings = FirebaseFirestoreSettings.Builder()
            .setCacheSizeBytes(25L * 1024 * 1024) // 25MB cap for offline persistence
            .build()
        firestore.firestoreSettings = settings
        return firestore
    }
}
