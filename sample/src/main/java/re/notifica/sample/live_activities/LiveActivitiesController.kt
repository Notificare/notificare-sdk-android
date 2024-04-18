package re.notifica.sample.live_activities

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.withContext
import re.notifica.Notificare
import re.notifica.ktx.events
import re.notifica.push.ktx.push
import re.notifica.sample.R
import re.notifica.sample.live_activities.models.CoffeeBrewerContentState
import re.notifica.sample.live_activities.ui.CoffeeLiveNotification
import re.notifica.sample.storage.datastore.NotificareDataStore
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

@SuppressLint("StaticFieldLeak")
object LiveActivitiesController {
    const val CHANNEL_LIVE_ACTIVITIES = "live-activities"

    private val notificationCounter = AtomicInteger(0)
    private lateinit var context: Context
    private lateinit var dataStore: NotificareDataStore

    private lateinit var _notificationManager: NotificationManager
    val notificationManager: NotificationManager
        get() = _notificationManager

    val coffeeActivityStream: Flow<CoffeeBrewerContentState?>
        get() = dataStore.coffeeBrewerContentStateStream

    fun setup(context: Context) {
        val applicationContext = context.applicationContext

        this.context = applicationContext
        this.dataStore = NotificareDataStore(applicationContext)
        this._notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun registerLiveActivitiesChannel() {
        val channel = NotificationChannel(
            CHANNEL_LIVE_ACTIVITIES,
            context.getString(R.string.notification_channel_live_activities_title),
            NotificationManager.IMPORTANCE_DEFAULT
        )

        channel.description =
            context.getString(R.string.notification_channel_live_activities_description)

        _notificationManager.createNotificationChannel(channel)
    }

    suspend fun handleTokenChanged(): Unit = withContext(Dispatchers.IO) {
        val coffeeBrewerContentState = coffeeActivityStream.lastOrNull()
        if (coffeeBrewerContentState != null) {
            Notificare.push().registerLiveActivity(LiveActivity.COFFEE_BREWER.identifier)
        }
    }

    // region Coffee Brewer

    suspend fun createCoffeeActivity(
        contentState: CoffeeBrewerContentState
    ): Unit = withContext(Dispatchers.IO) {
        // Present the notification UI.
        updateCoffeeActivity(contentState)

        // Track a custom event for analytics purposes.
        Notificare.events().logCustom(
            event = "live_activity_started",
            data = mapOf(
                "activity" to LiveActivity.COFFEE_BREWER.identifier,
                "activityId" to UUID.randomUUID().toString(),
            )
        )

        // Register on Notificare to receive updates.
        Notificare.push().registerLiveActivity(LiveActivity.COFFEE_BREWER.identifier)
    }

    suspend fun updateCoffeeActivity(
        contentState: CoffeeBrewerContentState
    ): Unit = withContext(Dispatchers.IO) {
        // Present the notification UI.
        val ongoingNotification = _notificationManager.activeNotifications
            .firstOrNull { it.tag == LiveActivity.COFFEE_BREWER.identifier }

        _notificationManager.notify(
            LiveActivity.COFFEE_BREWER.identifier,
            ongoingNotification?.id ?: notificationCounter.incrementAndGet(),
            CoffeeLiveNotification(context, contentState).build()
        )

        // Persist the state to storage.
        updateCoffeeBrewerState(contentState)
    }

    suspend fun clearCoffeeActivity(): Unit = withContext(Dispatchers.IO) {
        // Dismiss the notification.
        _notificationManager.activeNotifications
            .filter { it.tag == LiveActivity.COFFEE_BREWER.identifier }
            .forEach { _notificationManager.cancel(LiveActivity.COFFEE_BREWER.identifier, it.id) }

        // Persist the state to storage.
        updateCoffeeBrewerState(null)
        // End on Notificare to stop receiving updates.
        Notificare.push().endLiveActivity(LiveActivity.COFFEE_BREWER.identifier)
    }

    suspend fun updateCoffeeBrewerState(contentState: CoffeeBrewerContentState?) {
        dataStore.updateCoffeeBrewerContentState(contentState)
    }

    // endregion
}
