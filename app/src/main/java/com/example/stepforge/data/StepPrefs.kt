package com.example.stepforge.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

// Uygulama genelinde tek tane DataStore
val Context.stepforgeStore: DataStore<Preferences> by preferencesDataStore(name = "stepforge_prefs")