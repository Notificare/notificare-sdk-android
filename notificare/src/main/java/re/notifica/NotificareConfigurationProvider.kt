package re.notifica

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import androidx.core.os.bundleOf
import re.notifica.utilities.logging.NotificareLogger
import re.notifica.utilities.content.applicationInfo

/**
 * Auto configuration during application startup.
 */
internal class NotificareConfigurationProvider : ContentProvider() {

    private val logger = NotificareLogger(
        Notificare.options?.debugLoggingEnabled ?: false,
        "NotificareConfigurationProvider"
    )

    /**
     * Called before [android.app.Application.onCreate].
     */
    override fun onCreate(): Boolean {
        val context = context
            ?: throw IllegalStateException("Cannot find context from the provider.")

        if (hasAutoConfigurationEnabled(context)) {
            Notificare.configure(context)
            logger.info("Notificare configured automatically.")
        } else {
            logger.info(
                "Automatic configuration is disabled. Ensure you call configure() when the application starts."
            )
        }

        return true
    }

    override fun query(
        p0: Uri,
        p1: Array<out String>?,
        p2: String?,
        p3: Array<out String>?,
        p4: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int = 0

    private fun hasAutoConfigurationEnabled(context: Context): Boolean {
        val info = context.packageManager.applicationInfo(
            context.packageName,
            PackageManager.GET_META_DATA
        )

        val metadata = info.metaData ?: bundleOf()

        return metadata.getBoolean("re.notifica.auto_configuration_enabled", true)
    }
}
