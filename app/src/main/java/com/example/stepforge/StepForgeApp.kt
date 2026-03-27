package com.example.stepforge

import android.app.Application
import com.example.stepforge.debug.DebugInitializer

class StepForgeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DebugInitializer.init(this)
    }
}