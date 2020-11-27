package re.notifica.internal.storage.database.ktx

import com.squareup.moshi.Types
import re.notifica.Notificare
import re.notifica.internal.storage.database.entities.NotificareEventEntity
import re.notifica.models.NotificareEvent
import re.notifica.models.NotificareEventData

private val eventDataAdapter = Notificare.moshi.adapter<NotificareEventData>(
    Types.newParameterizedType(
        Map::class.java,
        String::class.java,
        String::class.java
    )
)

internal fun NotificareEvent.toEntity(): NotificareEventEntity {
    return NotificareEventEntity(
        type = this.type,
        timestamp = this.timestamp,
        deviceId = this.deviceId,
        sessionId = this.sessionId,
        notificationId = this.notificationId,
        userId = this.userId,
        data = eventDataAdapter.toJson(this.data),
        retries = 0,
        ttl = 86400, // 24 hours
    )
}

internal fun NotificareEventEntity.toModel(): NotificareEvent {
    return NotificareEvent(
        type = this.type,
        timestamp = this.timestamp,
        deviceId = this.deviceId,
        sessionId = this.sessionId,
        notificationId = this.notificationId,
        userId = this.userId,
        data = this.data?.let { eventDataAdapter.fromJson(it) }
    )
}
