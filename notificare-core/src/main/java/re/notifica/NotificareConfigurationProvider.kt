package re.notifica

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

/**
 * Auto configuration during application startup.
 */
class NotificareConfigurationProvider : ContentProvider() {
    /**
     * Called before [android.app.Application.onCreate].
     */
    override fun onCreate(): Boolean {
        val context =
            context ?: throw IllegalStateException("Cannot find context from the provider.")

        Notificare.configure(context)
        NotificareLogger.info("Notificare configured automatically.")

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
