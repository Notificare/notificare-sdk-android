package re.notifica

import re.notifica.ktx.device
import re.notifica.models.NotificareDevice
import re.notifica.models.NotificareDoNotDisturb
import re.notifica.models.NotificareUserData

public object NotificareDeviceCompat {

    @JvmStatic
    public val currentDevice: NotificareDevice?
        get() = Notificare.device().currentDevice

    @JvmStatic
    public val preferredLanguage: String?
        get() = Notificare.device().preferredLanguage

    @JvmStatic
    public fun register(userId: String?, userName: String?, callback: NotificareCallback<Unit>) {
        @Suppress("DEPRECATION")
        Notificare.device().register(userId, userName, callback)
    }

    @JvmStatic
    public fun updateUser(userId: String?, userName: String?, callback: NotificareCallback<Unit>) {
        Notificare.device().updateUser(userId, userName, callback)
    }

    @JvmStatic
    public fun updatePreferredLanguage(preferredLanguage: String?, callback: NotificareCallback<Unit>) {
        Notificare.device().updatePreferredLanguage(preferredLanguage, callback)
    }

    @JvmStatic
    public fun fetchTags(callback: NotificareCallback<List<String>>) {
        Notificare.device().fetchTags(callback)
    }

    @JvmStatic
    public fun addTag(tag: String, callback: NotificareCallback<Unit>) {
        Notificare.device().addTag(tag, callback)
    }

    @JvmStatic
    public fun addTags(tags: List<String>, callback: NotificareCallback<Unit>) {
        Notificare.device().addTags(tags, callback)
    }

    @JvmStatic
    public fun removeTag(tag: String, callback: NotificareCallback<Unit>) {
        Notificare.device().removeTag(tag, callback)
    }

    @JvmStatic
    public fun removeTags(tags: List<String>, callback: NotificareCallback<Unit>) {
        Notificare.device().removeTags(tags, callback)
    }

    @JvmStatic
    public fun clearTags(callback: NotificareCallback<Unit>) {
        Notificare.device().clearTags(callback)
    }

    @JvmStatic
    public fun fetchDoNotDisturb(callback: NotificareCallback<NotificareDoNotDisturb?>) {
        Notificare.device().fetchDoNotDisturb(callback)
    }

    @JvmStatic
    public fun updateDoNotDisturb(dnd: NotificareDoNotDisturb, callback: NotificareCallback<Unit>) {
        Notificare.device().updateDoNotDisturb(dnd, callback)
    }

    @JvmStatic
    public fun clearDoNotDisturb(callback: NotificareCallback<Unit>) {
        Notificare.device().clearDoNotDisturb(callback)
    }

    @JvmStatic
    public fun fetchUserData(callback: NotificareCallback<NotificareUserData>) {
        Notificare.device().fetchUserData(callback)
    }

    @JvmStatic
    public fun updateUserData(userData: NotificareUserData, callback: NotificareCallback<Unit>) {
        Notificare.device().updateUserData(userData, callback)
    }
}
