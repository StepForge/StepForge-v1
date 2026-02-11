package com.example.stepforge

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.stepforge.data.stepforgeStore
import com.example.stepforge.settings.SyncBackupScreen
import com.example.stepforge.ui.stepforgeTheme
import com.example.stepforge.ui.rememberUseDarkTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SyncBackupActivity : ComponentActivity() {

    private val KEY_BACKUP_EMAIL = stringPreferencesKey("backup_email")

    private lateinit var googleClient: GoogleSignInClient
    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (data == null) {
            Toast.makeText(this, "Google sign-in cancelled.", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }

        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            val email = account?.email
            val idToken = account?.idToken

            Log.d("SyncBackup", "GoogleSignIn account=$account email=$email idTokenNull=${idToken == null}")

            if (email != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    applicationContext.stepforgeStore.edit { prefs ->
                        prefs[KEY_BACKUP_EMAIL] = email
                    }
                }
            }

            if (idToken != null) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                firebaseAuth.signInWithCredential(credential)
                    .addOnCompleteListener { authTask ->
                        if (authTask.isSuccessful) {
                            val uid = firebaseAuth.currentUser?.uid
                            Log.d("SyncBackup", "FirebaseAuth success uid=$uid")
                            Toast.makeText(
                                this,
                                "Connected as ${email ?: "Google user"}",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Log.e("SyncBackup", "FirebaseAuth failed", authTask.exception)
                            Toast.makeText(this, "Firebase auth failed.", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(
                    this,
                    "Google account selected, but no ID token.",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: ApiException) {
            Log.e("SyncBackup", "GoogleSignIn failed: code=${e.statusCode}", e)
            if (e.statusCode == 12501) {
                Toast.makeText(this, "Google sign-in cancelled.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Google sign-in error (${e.statusCode})", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(getString(R.string.default_web_client_id))
            .build()
        googleClient = GoogleSignIn.getClient(this, gso)

        setContent {
            val useDark = rememberUseDarkTheme(this)
            stepforgeTheme(darkTheme = useDark) {
                SyncBackupScreen(
                    onBack = { finish() },
                    onSelectGoogleAccount = { launchGoogleAccountPickerForceChooser() },
                    onDisconnectGoogleAccount = { disconnectGoogleAccount() }
                )
            }
        }
    }

    /**
     * ✅ Change/Select account her zaman chooser açsın diye önce signOut + revokeAccess yapıyoruz.
     * Böylece GoogleSignIn cached hesabı otomatik seçemez.
     */
    private fun launchGoogleAccountPickerForceChooser() {
        Log.d("SyncBackup", "launchGoogleAccountPickerForceChooser()")

        // signOut -> revokeAccess -> signInIntent (best-effort)
        googleClient.signOut().addOnCompleteListener {
            googleClient.revokeAccess().addOnCompleteListener {
                val signInIntent: Intent = googleClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
        }
    }

    /**
     * ✅ Disconnect: sadece email'i silmek yetmez.
     * GoogleSignIn + FirebaseAuth oturumlarını kapatıp DataStore email'i de temizliyoruz.
     */
    private fun disconnectGoogleAccount() {
        Log.d("SyncBackup", "disconnectGoogleAccount()")

        // UI hemen "Not connected" görsün
        CoroutineScope(Dispatchers.IO).launch {
            applicationContext.stepforgeStore.edit { prefs ->
                prefs.remove(KEY_BACKUP_EMAIL)
            }
        }

        // Firebase sign out
        try {
            firebaseAuth.signOut()
        } catch (_: Exception) {
        }

        // Google sign out + revoke (best-effort)
        googleClient.signOut().addOnCompleteListener {
            googleClient.revokeAccess().addOnCompleteListener {
                Toast.makeText(this, "Disconnected.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}