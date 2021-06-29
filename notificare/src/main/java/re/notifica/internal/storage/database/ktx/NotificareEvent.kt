package re.notifica.internal.storage.database.ktx

import re.notifica.internal.storage.database.entities.NotificareEventEntity
import re.notifica.models.NotificareEvent

internal fun NotificareEvent.toEntity(): NotificareEventEntity {
    return NotificareEventEntity(
        type = this.type,
        timestamp = this.timestamp,
        deviceId = this.deviceId,
        sessionId = this.sessionId,
        notificationId = this.notificationId,
        userId = this.userId,
        data = NotificareEvent.dataAdapter.toJson(this.data),
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
        data = this.data?.let { NotificareEvent.dataAdapter.fromJson(it) }
    )
}
