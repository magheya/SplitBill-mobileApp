// MyApplication.kt
package com.example.myapplication

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.database.database
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Firebase.database.setPersistenceEnabled(true)
    }
}