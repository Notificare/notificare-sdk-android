package re.notifica.authentication.internal.storage.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import re.notifica.Notificare
import re.notifica.NotificareLogger
import re.notifica.authentication.internal.oauth.Credentials

internal class NotificareSharedPreferences(context: Context) {

    companion object {
        private const val PREFERENCES_FILE_NAME = "re.notifica.authentication.preferences"

        private const val PREFERENCE_CREDENTIALS = "re.notifica.authentication.preferences.credentials"
    }

    private val keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC
    private val mainKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec)

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        PREFERENCES_FILE_NAME,
        mainKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var credentials: Credentials?
        get() {
            return sharedPreferences.getString(PREFERENCE_CREDENTIALS, null)
                ?.let {
                    try {
                        Notificare.moshi.adapter(Credentials::class.java).fromJson(it)
                    } catch (e: Exception) {
                        NotificareLogger.warning("Failed to decode the stored credential.", e)

                        // Remove the corrupted device from local storage.
                        credentials = null

                        null
                    }
                }
        }
        set(value) {
            sharedPreferences.edit().also {
                if (value == null) it.remove(PREFERENCE_CREDENTIALS)
                else it.putString(
                    PREFERENCE_CREDENTIALS,
                    Notificare.moshi.adapter(Credentials::class.java).toJson(value)
                )
            }.apply()
        }
}
