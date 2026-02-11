package com.example.stepforge.backup

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.stepforge.data.AppDatabase
import com.example.stepforge.data.DailySteps
import com.example.stepforge.data.DailyWater
import com.example.stepforge.data.HourlySteps
import com.example.stepforge.data.SleepSession
import com.example.stepforge.data.SleepStage
import com.example.stepforge.data.stepforgeStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class CloudBackupManager(private val context: Context) {

    enum class BackupVersion(val code: Int) {
        V1(1)
    }

    enum class RestoreResult {
        SUCCESS,
        NO_BACKUP,
        CORRUPT_DATA,
        NETWORK_ERROR,
        UNKNOWN_ERROR
    }

    enum class DeleteCloudResult {
        SUCCESS,
        NO_SIGNED_IN_USER,
        NETWORK_ERROR,
        UNKNOWN_ERROR
    }

    companion object {
        private const val TAG = "stepforgeCloudBackup"
    }

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // --------- DataStore keys ---------

    // Profil
    private val KEY_USERNAME = stringPreferencesKey("username")
    private val KEY_GENDER = stringPreferencesKey("gender")
    private val KEY_BIRTH = stringPreferencesKey("birth_date")
    private val KEY_HEIGHT = intPreferencesKey("height")
    private val KEY_WEIGHT = intPreferencesKey("weight")

    // Ayarlar
    private val KEY_UNIT = stringPreferencesKey("unit")
    private val KEY_STEP_GOAL = intPreferencesKey("step_goal")
    private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    private val KEY_NOTIF_TIME = stringPreferencesKey("notif_time")
    private val KEY_SYNC_AUTO = intPreferencesKey("sync_auto_enabled")

    // --------- PUBLIC API ---------

    suspend fun uploadToCloud(): Boolean = withContext(Dispatchers.IO) {
        try {
            val uid = ensureUser()
            val json = buildBackupJson()

            val doc = mapOf(
                "version" to json.getInt("version"),
                "generatedAt" to json.getLong("generatedAt"),
                "payload" to json.toString()
            )

            db.collection("users")
                .document(uid)
                .collection("backups")
                .document("latest")
                .set(doc)
                .await()

            Log.d(TAG, "Cloud backup uploaded for uid=$uid")
            true
        } catch (e: Exception) {
            Log.e(TAG, "uploadToCloud error", e)
            false
        }
    }

    suspend fun downloadFromCloud(): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val uid = ensureUser()
            val snap = db.collection("users")
                .document(uid)
                .collection("backups")
                .document("latest")
                .get()
                .await()

            if (!snap.exists()) {
                Log.d(TAG, "No backup found for uid=$uid")
                return@withContext null
            }

            val payload = snap.getString("payload") ?: return@withContext null
            JSONObject(payload)
        } catch (e: Exception) {
            Log.e(TAG, "downloadFromCloud error", e)
            null
        }
    }

    /**
     * Cloud'daki "latest" yedeği indirir ve
     *  - DataStore (profil + bazı ayarlar)
     *  - Room (daily_steps + hourly_steps + daily_water + sleep)
     * içine geri yazar.
     */
    suspend fun restoreFromCloud(): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val root = downloadFromCloud() ?: run {
                Log.d(TAG, "restoreFromCloud: no payload")
                return@withContext RestoreResult.NO_BACKUP
            }

            val versionCode = root.optInt("version", 1)
            val version =
                BackupVersion.values().firstOrNull { it.code == versionCode } ?: BackupVersion.V1
            Log.d(TAG, "restoreFromCloud: using version=$versionCode")

            when (version) {
                BackupVersion.V1 -> {
                    // 1) Preferences
                    val prefsJson = root.optJSONObject("preferences")
                    if (prefsJson != null) {
                        try {
                            restorePreferences(prefsJson)
                        } catch (e: Exception) {
                            Log.e(TAG, "restorePreferences error", e)
                            return@withContext RestoreResult.CORRUPT_DATA
                        }
                    }

                    // 2) DailySteps
                    val stepsArray = root.optJSONArray("dailySteps")
                    if (stepsArray != null) {
                        try {
                            restoreDailySteps(stepsArray)
                        } catch (e: Exception) {
                            Log.e(TAG, "restoreDailySteps error", e)
                            return@withContext RestoreResult.CORRUPT_DATA
                        }
                    }

                    // 3) HourlySteps
                    val hourlyArray = root.optJSONArray("hourlySteps")
                    if (hourlyArray != null) {
                        try {
                            restoreHourlySteps(hourlyArray)
                        } catch (e: Exception) {
                            Log.e(TAG, "restoreHourlySteps error", e)
                            return@withContext RestoreResult.CORRUPT_DATA
                        }
                    }

                    // 4) DailyWater
                    val waterArray = root.optJSONArray("dailyWater")
                    if (waterArray != null) {
                        try {
                            restoreDailyWater(waterArray)
                        } catch (e: Exception) {
                            Log.e(TAG, "restoreDailyWater error", e)
                            return@withContext RestoreResult.CORRUPT_DATA
                        }
                    }

                    // 5) SleepSessions
                    val sleepArray = root.optJSONArray("sleepSessions")
                    if (sleepArray != null) {
                        try {
                            restoreSleep(sleepArray)
                        } catch (e: Exception) {
                            Log.e(TAG, "restoreSleep error", e)
                            return@withContext RestoreResult.CORRUPT_DATA
                        }
                    }

                    Log.d(TAG, "restoreFromCloud completed")
                    RestoreResult.SUCCESS
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "restoreFromCloud error", e)
            if (e is UnknownHostException || e is SocketTimeoutException) {
                RestoreResult.NETWORK_ERROR
            } else {
                RestoreResult.UNKNOWN_ERROR
            }
        }
    }

    suspend fun deleteCloudBackupsAndSignOut(): DeleteCloudResult = withContext(Dispatchers.IO) {
        try {
            val user = auth.currentUser ?: run {
                Log.d(TAG, "deleteCloudBackupsAndSignOut: no currentUser")
                return@withContext DeleteCloudResult.NO_SIGNED_IN_USER
            }

            val uid = user.uid
            Log.d(TAG, "deleteCloudBackupsAndSignOut: uid=$uid")

            val backupsRef = db.collection("users").document(uid).collection("backups")
            val snaps = backupsRef.get().await()
            for (doc in snaps.documents) {
                try {
                    doc.reference.delete().await()
                } catch (e: Exception) {
                    Log.e(TAG, "delete doc failed: ${doc.reference.path}", e)
                    throw e
                }
            }

            try {
                db.collection("users").document(uid).delete().await()
            } catch (_: Exception) {
            }

            try {
                user.delete().await()
                Log.d(TAG, "Firebase user deleted")
            } catch (e: Exception) {
                Log.w(
                    TAG,
                    "Firebase user delete failed (likely requires recent login). Proceeding with signOut.",
                    e
                )
            }

            auth.signOut()
            DeleteCloudResult.SUCCESS
        } catch (e: Exception) {
            Log.e(TAG, "deleteCloudBackupsAndSignOut error", e)
            if (e is UnknownHostException || e is SocketTimeoutException) {
                DeleteCloudResult.NETWORK_ERROR
            } else {
                DeleteCloudResult.UNKNOWN_ERROR
            }
        }
    }

    // --------- INTERNAL HELPERS ---------

    private suspend fun ensureUser(): String = withContext(Dispatchers.IO) {
        val current = auth.currentUser
        if (current != null) {
            Log.d(TAG, "Using existing user uid=${current.uid}, email=${current.email}")
            return@withContext current.uid
        }

        val result = auth.signInAnonymously().await()
        val uid = result.user?.uid ?: throw IllegalStateException("No user after signInAnonymously")
        Log.d(TAG, "Anonymous user created: $uid")
        uid
    }

    /** DataStore + Room'dan tüm veriyi JSON objesine dönüştürür. */
    private suspend fun buildBackupJson(): JSONObject = withContext(Dispatchers.IO) {
        val prefs = context.stepforgeStore.data.first()
        val prefsJson = preferencesToJson(prefs)

        val db = AppDatabase.getDatabase(context)

        // daily_steps
        val dailyDao = db.dailyStepsDao()
        val stepsList = dailyDao.getAllSteps()

        // hourly_steps
        val hourlyDao = db.hourlyStepsDao()
        val hourlyList = hourlyDao.getAll()

        // daily_water
        val waterDao = db.dailyWaterDao()
        val waterList = waterDao.getAllWater()

        // sleep_session + sleep_stage
        val sleepSessionDao = db.sleepSessionDao()
        val sleepStageDao = db.sleepStageDao()
        val sleepSessions = sleepSessionDao.getRecentSessions(365)
        val stagesBySessionId = mutableMapOf<Long, List<SleepStage>>()
        sleepSessions.forEach { session ->
            stagesBySessionId[session.id] = sleepStageDao.getStagesForSession(session.id)
        }

        val stepsArray = JSONArray().apply {
            stepsList.forEach { daily ->
                put(
                    JSONObject().apply {
                        put("date", daily.date)
                        put("steps", daily.steps)
                    }
                )
            }
        }

        val compressedHourly: List<HourlySteps> =
            hourlyList
                .groupBy { it.date to it.hour }
                .mapValues { (_, items) -> items.maxByOrNull { it.steps }!! }
                .values
                .sortedWith(compareBy<HourlySteps>({ it.date }, { it.hour }))

        val hourlyArray = JSONArray().apply {
            compressedHourly.forEach { h ->
                put(
                    JSONObject().apply {
                        put("date", h.date)
                        put("hour", h.hour)
                        put("steps", h.steps)
                    }
                )
            }
        }

        val waterArray = JSONArray().apply {
            waterList.forEach { w ->
                put(
                    JSONObject().apply {
                        put("date", w.date)
                        put("water_ml", w.waterMl)
                    }
                )
            }
        }

        val sleepArray = JSONArray().apply {
            sleepSessions.forEach { s ->
                put(
                    JSONObject().apply {
                        put("id", s.id)
                        put("date", s.date)
                        put("startTime", s.startTime)
                        put("endTime", s.endTime)
                        put("totalMinutes", s.totalMinutes)
                        put("qualityScore", s.qualityScore)
                        put("source", s.source)

                        val stages = stagesBySessionId[s.id].orEmpty()
                        val stagesJson = JSONArray().apply {
                            stages.forEach { st ->
                                put(
                                    JSONObject().apply {
                                        put("id", st.id)
                                        put("sessionId", st.sessionId)
                                        put("stageType", st.stageType)
                                        put("startTime", st.startTime)
                                        put("endTime", st.endTime)
                                    }
                                )
                            }
                        }
                        put("stages", stagesJson)
                    }
                )
            }
        }

        JSONObject().apply {
            put("version", BackupVersion.V1.code)
            put("generatedAt", System.currentTimeMillis())
            put("preferences", prefsJson)
            put("dailySteps", stepsArray)
            put("hourlySteps", hourlyArray)
            put("dailyWater", waterArray)
            put("sleepSessions", sleepArray)
        }
    }

    /**
     * Preferences nesnesini JSON'a map eder.
     * Profil + temel ayarlar birlikte gidiyor.
     */
    private fun preferencesToJson(prefs: Preferences): JSONObject {
        return JSONObject().apply {
            // Profil
            put("username", prefs[KEY_USERNAME] ?: JSONObject.NULL)
            put("gender", prefs[KEY_GENDER] ?: JSONObject.NULL)
            put("birth_date", prefs[KEY_BIRTH] ?: JSONObject.NULL)
            put("height", prefs[KEY_HEIGHT] ?: -1)
            put("weight", prefs[KEY_WEIGHT] ?: -1)

            // Ayarlar
            put("unit", prefs[KEY_UNIT] ?: "km")
            put("step_goal", prefs[KEY_STEP_GOAL] ?: 10000)
            put("theme_mode", prefs[KEY_THEME_MODE] ?: "system")
            put("notif_time", prefs[KEY_NOTIF_TIME] ?: "09:00")
            put("sync_auto_enabled", prefs[KEY_SYNC_AUTO] ?: 0)
        }
    }

    /** JSON -> DataStore (profil + temel ayarlar) */
    private suspend fun restorePreferences(p: JSONObject) {
        context.stepforgeStore.edit { prefs ->
            if (p.has("username") && !p.isNull("username")) {
                prefs[KEY_USERNAME] = p.optString("username", "")
            }
            if (p.has("gender") && !p.isNull("gender")) {
                prefs[KEY_GENDER] = p.optString("gender", "")
            }
            if (p.has("birth_date") && !p.isNull("birth_date")) {
                prefs[KEY_BIRTH] = p.optString("birth_date", "")
            }
            if (p.has("height")) {
                prefs[KEY_HEIGHT] = p.optInt("height", -1)
            }
            if (p.has("weight")) {
                prefs[KEY_WEIGHT] = p.optInt("weight", -1)
            }

            if (!p.isNull("unit")) prefs[KEY_UNIT] = p.optString("unit", "km")
            if (p.has("step_goal")) prefs[KEY_STEP_GOAL] = p.optInt("step_goal", 10000)
            if (!p.isNull("theme_mode")) prefs[KEY_THEME_MODE] = p.optString("theme_mode", "system")
            if (!p.isNull("notif_time")) prefs[KEY_NOTIF_TIME] = p.optString("notif_time", "09:00")
            if (p.has("sync_auto_enabled")) prefs[KEY_SYNC_AUTO] = p.optInt("sync_auto_enabled", 0)
        }
    }

    /** JSON array -> Room.daily_steps */
    private suspend fun restoreDailySteps(array: JSONArray) {
        val dao = AppDatabase.getDatabase(context).dailyStepsDao()
        val list = mutableListOf<DailySteps>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val date = obj.optString("date", "")
            val steps = obj.optInt("steps", 0)
            if (date.isNotEmpty()) {
                list.add(DailySteps(date = date, steps = steps))
            }
        }
        list.forEach { dao.insertDailySteps(it) }
    }

    /** JSON array -> Room.hourly_steps */
    private suspend fun restoreHourlySteps(array: JSONArray) {
        val dao = AppDatabase.getDatabase(context).hourlyStepsDao()
        dao.clearAll()

        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val date = obj.optString("date", "")
            val hour = obj.optInt("hour", -1)
            val steps = obj.optInt("steps", 0)

            if (date.isNotEmpty() && hour in 0..23) {
                dao.upsert(
                    HourlySteps(
                        date = date,
                        hour = hour,
                        steps = steps
                    )
                )
            }
        }
    }

    /** JSON array -> Room.daily_water */
    private suspend fun restoreDailyWater(array: JSONArray) {
        val dao = AppDatabase.getDatabase(context).dailyWaterDao()
        val list = mutableListOf<DailyWater>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val date = obj.optString("date", "")
            val waterMl = obj.optInt("water_ml", 0)
            if (date.isNotEmpty()) {
                list.add(DailyWater(date = date, waterMl = waterMl))
            }
        }
        list.forEach { dao.insertDailyWater(it) }
    }

    /** JSON array -> Room.sleep_session + sleep_stage */
    private suspend fun restoreSleep(array: JSONArray) {
        val db = AppDatabase.getDatabase(context)
        val sessionDao = db.sleepSessionDao()
        val stageDao = db.sleepStageDao()

        val sessions = mutableListOf<SleepSession>()

        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val date = obj.optString("date", "")
            if (date.isEmpty()) continue

            val startTime = obj.optLong("startTime", 0L)
            val endTime = obj.optLong("endTime", 0L)
            val totalMinutes = obj.optInt("totalMinutes", 0)
            val qualityScore = obj.optInt("qualityScore", 0)
            val source = obj.optString("source", "manual")

            sessions.add(
                SleepSession(
                    id = 0,
                    date = date,
                    startTime = startTime,
                    endTime = endTime,
                    totalMinutes = totalMinutes,
                    qualityScore = qualityScore,
                    source = source
                )
            )
        }

        // Eski kayıtları temizle (date bazlı)
        sessions.map { it.date }.distinct().forEach { d ->
            sessionDao.deleteByDate(d)
        }

        // Yeni session + stage'leri ekle
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val date = obj.optString("date", "")
            if (date.isEmpty()) continue

            val startTime = obj.optLong("startTime", 0L)
            val endTime = obj.optLong("endTime", 0L)
            val totalMinutes = obj.optInt("totalMinutes", 0)
            val qualityScore = obj.optInt("qualityScore", 0)
            val source = obj.optString("source", "manual")

            val sessionId = sessionDao.insert(
                SleepSession(
                    id = 0,
                    date = date,
                    startTime = startTime,
                    endTime = endTime,
                    totalMinutes = totalMinutes,
                    qualityScore = qualityScore,
                    source = source
                )
            )

            val stagesArray = obj.optJSONArray("stages") ?: continue
            for (j in 0 until stagesArray.length()) {
                val stObj = stagesArray.optJSONObject(j) ?: continue
                val stageType = stObj.optString("stageType", "unknown")
                val stStart = stObj.optLong("startTime", 0L)
                val stEnd = stObj.optLong("endTime", 0L)

                stageDao.insert(
                    SleepStage(
                        id = 0,
                        sessionId = sessionId,
                        stageType = stageType,
                        startTime = stStart,
                        endTime = stEnd
                    )
                )
            }
        }
    }
}