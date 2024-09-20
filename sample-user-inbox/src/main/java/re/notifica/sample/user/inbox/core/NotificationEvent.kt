package re.notifica.sample.user.inbox.core

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

internal object NotificationEvent {
    private val _inboxShouldUpdateFlow = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val inboxShouldUpdateFlow: SharedFlow<Unit> = _inboxShouldUpdateFlow

    internal fun triggerInboxShouldUpdateEvent() {
        _inboxShouldUpdateFlow.tryEmit(Unit)
    }
}
