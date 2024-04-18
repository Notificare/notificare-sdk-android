package re.notifica

import re.notifica.models.NotificareDevice
import re.notifica.models.NotificareDoNotDisturb
import re.notifica.models.NotificareTransport
import re.notifica.models.NotificareUserData

public interface NotificareDeviceModule {

    public val currentDevice: NotificareDevice?

    public val preferredLanguage: String?

    public suspend fun register(userId: String?, userName: String?)

    public fun register(userId: String?, userName: String?, callback: NotificareCallback<Unit>)

    public suspend fun updatePreferredLanguage(preferredLanguage: String?)

    public fun updatePreferredLanguage(preferredLanguage: String?, callback: NotificareCallback<Unit>)

    public suspend fun fetchTags(): List<String>

    public fun fetchTags(callback: NotificareCallback<List<String>>)

    public suspend fun addTag(tag: String)

    public fun addTag(tag: String, callback: NotificareCallback<Unit>)

    public suspend fun addTags(tags: List<String>)

    public fun addTags(tags: List<String>, callback: NotificareCallback<Unit>)

    public suspend fun removeTag(tag: String)

    public fun removeTag(tag: String, callback: NotificareCallback<Unit>)

    public suspend fun removeTags(tags: List<String>)

    public fun removeTags(tags: List<String>, callback: NotificareCallback<Unit>)

    public suspend fun clearTags()

    public fun clearTags(callback: NotificareCallback<Unit>)

    public suspend fun fetchDoNotDisturb(): NotificareDoNotDisturb?

    public fun fetchDoNotDisturb(callback: NotificareCallback<NotificareDoNotDisturb?>)

    public suspend fun updateDoNotDisturb(dnd: NotificareDoNotDisturb)

    public fun updateDoNotDisturb(dnd: NotificareDoNotDisturb, callback: NotificareCallback<Unit>)

    public suspend fun clearDoNotDisturb()

    public fun clearDoNotDisturb(callback: NotificareCallback<Unit>)

    public suspend fun fetchUserData(): NotificareUserData

    public fun fetchUserData(callback: NotificareCallback<NotificareUserData>)

    public suspend fun updateUserData(userData: NotificareUserData)

    public fun updateUserData(userData: NotificareUserData, callback: NotificareCallback<Unit>)
}

public interface NotificareInternalDeviceModule {

    @InternalNotificareApi
    public suspend fun registerTemporary()

    @InternalNotificareApi
    public suspend fun registerPushToken(transport: NotificareTransport, token: String)
}
