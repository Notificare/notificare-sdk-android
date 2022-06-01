package re.notifica

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import re.notifica.internal.NotificareLogger
import re.notifica.ktx.session

/**
 * Auto configuration during application startup.
 */
internal class NotificareSessionConfigurationProvider : ContentProvider() {
    /**
     * Called before [android.app.Application.onCreate].
     */
    override fun onCreate(): Boolean {
        val context = context ?: throw IllegalStateException("Cannot find context from the provider.")
        val application = context.applicationContext as Application

        NotificareLogger.debug("Configuring session lifecycle listeners.")
        Notificare.session().setupLifecycleListeners(application)

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

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int = 0
}
