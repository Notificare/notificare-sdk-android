package re.notifica

import re.notifica.ktx.device
import re.notifica.models.NotificareDevice
import re.notifica.models.NotificareDoNotDisturb
import re.notifica.models.NotificareUserData

public object NotificareDeviceCompat {

    /**
     * Provides the current registered device information.
     */
    @JvmStatic
    public val currentDevice: NotificareDevice?
        get() = Notificare.device().currentDevice

    /**
     * Provides the preferred language of the current device for notifications and messages.
     */
    @JvmStatic
    public val preferredLanguage: String?
        get() = Notificare.device().preferredLanguage

    /**
     * Registers the device with an associated user, with a callback.
     *
     * To register the device anonymously, set both `userId` and `userName` to `null`.
     *
     * @param userId Optional unique identifier for the user.
     * @param userName Optional display name for the user.
     * @param callback The callback invoked upon completion of the registration.
     */
    @JvmStatic
    public fun register(userId: String?, userName: String?, callback: NotificareCallback<Unit>) {
        @Suppress("DEPRECATION")
        Notificare.device().register(userId, userName, callback)
    }

    /**
     * Updates the user information for the device, with a callback.
     *
     * To register the device anonymously, set both `userId` and `userName` to `null`.
     *
     * @param userId Optional user identifier.
     * @param userName Optional user name.
     */
    @JvmStatic
    public fun updateUser(userId: String?, userName: String?, callback: NotificareCallback<Unit>) {
        Notificare.device().updateUser(userId, userName, callback)
    }

    /**
     * Updates the preferred language setting for the device, with a callback.
     *
     * @param preferredLanguage The preferred language code.
     * @param callback The callback handling the update result.
     */
    @JvmStatic
    public fun updatePreferredLanguage(preferredLanguage: String?, callback: NotificareCallback<Unit>) {
        Notificare.device().updatePreferredLanguage(preferredLanguage, callback)
    }

    /**
     * Fetches the tags associated with the device, with a callback.
     *
     * @param callback The callback handling the tags retrieval result.
     */
    @JvmStatic
    public fun fetchTags(callback: NotificareCallback<List<String>>) {
        Notificare.device().fetchTags(callback)
    }

    /**
     * Adds a single tag to the device, with a callback.
     *
     * @param tag The tag to add.
     * @param callback The callback handling the tag addition result.
     */
    @JvmStatic
    public fun addTag(tag: String, callback: NotificareCallback<Unit>) {
        Notificare.device().addTag(tag, callback)
    }

    /**
     * Adds multiple tags to the device, with a callback.
     *
     * @param tags A list of tags to add.
     * @param callback The callback handling the tags addition result.
     */
    @JvmStatic
    public fun addTags(tags: List<String>, callback: NotificareCallback<Unit>) {
        Notificare.device().addTags(tags, callback)
    }

    /**
     * Removes a specific tag from the device, with a callback.
     *
     * @param tag The tag to remove.
     * @param callback The callback handling the tag removal result.
     */
    @JvmStatic
    public fun removeTag(tag: String, callback: NotificareCallback<Unit>) {
        Notificare.device().removeTag(tag, callback)
    }

    /**
     * Removes multiple tags from the device, with a callback.
     *
     * @param tags A list of tags to remove.
     * @param callback The callback handling the tags removal result.
     */
    @JvmStatic
    public fun removeTags(tags: List<String>, callback: NotificareCallback<Unit>) {
        Notificare.device().removeTags(tags, callback)
    }

    /**
     * Clears all tags from the device, with a callback.
     *
     * @param callback The callback handling the tags clearance result.
     */
    @JvmStatic
    public fun clearTags(callback: NotificareCallback<Unit>) {
        Notificare.device().clearTags(callback)
    }

    /**
     * Fetches the "Do Not Disturb" (DND) settings for the device, with a callback.
     *
     * @param callback The callback handling the DND retrieval result.
     *
     * @see [NotificareDoNotDisturb]
     */
    @JvmStatic
    public fun fetchDoNotDisturb(callback: NotificareCallback<NotificareDoNotDisturb?>) {
        Notificare.device().fetchDoNotDisturb(callback)
    }

    /**
     * Updates the "Do Not Disturb" (DND) settings for the device, with a callback.
     *
     * @param dnd The new DND settings to apply.
     * @param callback The callback handling the DND update result.
     *
     * @see [NotificareDoNotDisturb]
     */
    @JvmStatic
    public fun updateDoNotDisturb(dnd: NotificareDoNotDisturb, callback: NotificareCallback<Unit>) {
        Notificare.device().updateDoNotDisturb(dnd, callback)
    }

    /**
     * Clears the "Do Not Disturb" (DND) settings for the device, with a callback.
     *
     * @param callback The callback handling the DND clearance result.
     */
    @JvmStatic
    public fun clearDoNotDisturb(callback: NotificareCallback<Unit>) {
        Notificare.device().clearDoNotDisturb(callback)
    }

    /**
     * Fetches the user data associated with the device, with a callback.
     *
     * @param callback The callback handling the user data retrieval result.
     */
    @JvmStatic
    public fun fetchUserData(callback: NotificareCallback<NotificareUserData>) {
        Notificare.device().fetchUserData(callback)
    }

    /**
     * Updates the custom user data associated with the device, with a callback.
     *
     * @param userData The updated user data to associate with the device.
     * @param callback The callback handling the user data update result.
     */
    @JvmStatic
    public fun updateUserData(userData: NotificareUserData, callback: NotificareCallback<Unit>) {
        Notificare.device().updateUserData(userData, callback)
    }
}
