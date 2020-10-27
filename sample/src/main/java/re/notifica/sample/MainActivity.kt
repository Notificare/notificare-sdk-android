package re.notifica.sample

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.runBlocking
import re.notifica.Notificare
import re.notifica.models.NotificareDoNotDisturb
import re.notifica.models.NotificareTime
import java.util.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun onFetchTagsClick(@Suppress("UNUSED_PARAMETER") view: View) {
        runBlocking {
            val tags = Notificare.deviceManager.fetchTags()
            Log.i(TAG, "$tags")
        }

//        runBlocking {
//            // Notificare.eventsManager.logApplicationOpen()
//            Notificare.eventsManager.logCustom("hello", mapOf(
//                Pair("name", "Helder"),
//            ))
//        }
    }

    fun onAddTagsClick(@Suppress("UNUSED_PARAMETER") view: View) {
        runBlocking {
            Notificare.deviceManager.addTags(
                listOf(
                    "hpinhal",
                    "android",
                    "remove-me",
                )
            )
        }
    }

    fun onRemoveTagsClick(@Suppress("UNUSED_PARAMETER") view: View) {
        runBlocking {
            Notificare.deviceManager.removeTag("remove-me")
        }
    }

    fun onClearTagsClick(@Suppress("UNUSED_PARAMETER") view: View) {
        runBlocking {
            Notificare.deviceManager.clearTags()
        }
    }

    fun onFetchDndClick(@Suppress("UNUSED_PARAMETER") view: View) {
        runBlocking {
            val dnd = Notificare.deviceManager.fetchDoNotDisturb()
            Log.i(TAG, "$dnd")
        }
    }

    fun onUpdateDndClick(@Suppress("UNUSED_PARAMETER") view: View) {
        runBlocking {
            Notificare.deviceManager.updateDoNotDisturb(
                NotificareDoNotDisturb(
                    start = NotificareTime("00:00"),
                    end = NotificareTime(8, Calendar.getInstance().get(Calendar.MINUTE))
                )
            )
        }
    }

    fun onClearDndClick(@Suppress("UNUSED_PARAMETER") view: View) {
        runBlocking {
            Notificare.deviceManager.clearDoNotDisturb()
        }
    }

    fun onFetchUserDataClick(@Suppress("UNUSED_PARAMETER") view: View) {
        runBlocking {
            val userData = Notificare.deviceManager.fetchUserData()
            Log.i(TAG, "$userData")
        }
    }

    fun onUpdateUserDataClick(@Suppress("UNUSED_PARAMETER") view: View) {
        runBlocking {
            Notificare.deviceManager.updateUserData(
                mapOf(
                    Pair("firstName", "Helder"),
                    Pair("lastName", "Pinhal"),
                )
            )
        }
    }

    fun onUpdatePreferredLanguage(@Suppress("UNUSED_PARAMETER") view: View) {
        runBlocking {
            Notificare.deviceManager.updatePreferredLanguage("en-NL")
        }
    }

    fun onClearPreferredLanguage(@Suppress("UNUSED_PARAMETER") view: View) {
        runBlocking {
            Notificare.deviceManager.updatePreferredLanguage(null)
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }
}
