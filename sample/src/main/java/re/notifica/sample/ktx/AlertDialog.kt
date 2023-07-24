package re.notifica.sample.ktx

import android.content.Context
import androidx.appcompat.app.AlertDialog
import re.notifica.sample.R

fun showBasicAlert(context: Context, message: String) {
    AlertDialog.Builder(context)
        .setTitle(R.string.app_name)
        .setMessage(message)
        .setCancelable(false)
        .setNegativeButton(R.string.dialog_ok_button) { _, _ ->
        }
        .show()
}
