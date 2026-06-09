package com.example.stepforge

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.intPreferencesKey
import com.example.stepforge.data.AppDatabase
import com.example.stepforge.data.stepforgeStore
import com.example.stepforge.ui.history.HistoryRoute
import com.example.stepforge.ui.rememberUseDarkTheme
import com.example.stepforge.ui.stepforgeTheme
import kotlinx.coroutines.flow.map

class HistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWeatherLocationPermissionIfNeeded()

        val db = AppDatabase.getDatabase(this)
        val stepsDao = db.dailyStepsDao()
        val waterDao = db.dailyWaterDao()
        val sleepDao = db.sleepSessionDao()
        val workoutDao = db.workoutSessionDao()

        setContent {
            val context = LocalContext.current
            val stepsHistory by stepsDao.observeAllSteps().collectAsState(initial = emptyList())
            val waterHistory by waterDao.observeAllWater().collectAsState(initial = emptyList())
            val workouts by workoutDao.observeAll().collectAsState(initial = emptyList())
            val stepGoalKey = remember { intPreferencesKey("step_goal") }
            val waterGoalKey = remember { intPreferencesKey("water_goal_ml") }
            val stepGoal by remember {
                context.stepforgeStore.data.map { prefs -> prefs[stepGoalKey] ?: 10_000 }
            }.collectAsState(initial = 10_000)
            val waterGoal by remember {
                context.stepforgeStore.data.map { prefs -> prefs[waterGoalKey] ?: 2_000 }
            }.collectAsState(initial = 2_000)

            stepforgeTheme(darkTheme = rememberUseDarkTheme(this)) {
                HistoryRoute(
                    activity = this@HistoryActivity,
                    stepsHistory = stepsHistory,
                    waterHistory = waterHistory,
                    workouts = workouts,
                    sleepDao = sleepDao,
                    stepGoal = stepGoal.coerceAtLeast(1),
                    waterGoal = waterGoal.coerceAtLeast(1),
                    onBack = { finish() }
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == WEATHER_LOCATION_PERMISSION_REQUEST && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
            recreate()
        }
    }

    private fun requestWeatherLocationPermissionIfNeeded() {
        val hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasCoarse && !hasFine) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                WEATHER_LOCATION_PERMISSION_REQUEST
            )
        }
    }

    private companion object {
        const val WEATHER_LOCATION_PERMISSION_REQUEST = 3441
    }

}
