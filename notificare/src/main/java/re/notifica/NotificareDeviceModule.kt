package re.notifica

import re.notifica.models.NotificareDevice
import re.notifica.models.NotificareDoNotDisturb
import re.notifica.models.NotificareUserData

public interface NotificareDeviceModule {

    /**
     * Provides the current registered device information.
     */
    public val currentDevice: NotificareDevice?

    /**
     * Provides the preferred language of the current device for notifications and messages.
     */
    public val preferredLanguage: String?

    /**
     * Registers a user for the device.
     *
     * To register the device anonymously, set both `userId` and `userName` to `null`.
     *
     * @param userId Optional user identifier.
     * @param userName Optional user name.
     */
    @Deprecated(
        message = "Use updateUser() instead.",
        replaceWith = ReplaceWith("updateUser(userId, userName)")
    )
    public suspend fun register(userId: String?, userName: String?)

    /**
     * Registers the device with an associated user, with a callback.
     *
     * To register the device anonymously, set both `userId` and `userName` to `null`.
     *
     * @param userId Optional unique identifier for the user.
     * @param userName Optional display name for the user.
     * @param callback The callback invoked upon completion of the registration.
     */
    @Deprecated(
        message = "Use updateUser() instead.",
        replaceWith = ReplaceWith("updateUser(userId, userName, callback)")
    )
    public fun register(userId: String?, userName: String?, callback: NotificareCallback<Unit>)

    /**
     * Updates the user information for the device.
     *
     * To register the device anonymously, set both `userId` and `userName` to `null`.
     *
     * @param userId Optional user identifier.
     * @param userName Optional user name.
     */
    public suspend fun updateUser(userId: String?, userName: String?)

    /**
     * Updates the user information for the device, with a callback.
     *
     * To register the device anonymously, set both `userId` and `userName` to `null`.
     *
     * @param userId Optional user identifier.
     * @param userName Optional user name.
     */
    public fun updateUser(userId: String?, userName: String?, callback: NotificareCallback<Unit>)

    /**
     * Updates the preferred language setting for the device.
     *
     * @param preferredLanguage The preferred language code.
     */
    public suspend fun updatePreferredLanguage(preferredLanguage: String?)

    /**
     * Updates the preferred language setting for the device, with a callback.
     *
     * @param preferredLanguage The preferred language code.
     * @param callback The callback handling the update result.
     */
    public fun updatePreferredLanguage(preferredLanguage: String?, callback: NotificareCallback<Unit>)

    /**
     * Fetches the tags associated with the device.
     *
     * @return A list of tags currently associated with the device.
     */
    public suspend fun fetchTags(): List<String>

    /**
     * Fetches the tags associated with the device, with a callback.
     *
     * @param callback The callback handling the tags retrieval result.
     */
    public fun fetchTags(callback: NotificareCallback<List<String>>)

    /**
     * Adds a single tag to the device.
     *
     * @param tag The tag to add.
     */
    public suspend fun addTag(tag: String)

    /**
     * Adds a single tag to the device, with a callback.
     *
     * @param tag The tag to add.
     * @param callback The callback handling the tag addition result.
     */
    public fun addTag(tag: String, callback: NotificareCallback<Unit>)

    /**
     * Adds multiple tags to the device.
     *
     * @param tags A list of tags to add.
     */
    public suspend fun addTags(tags: List<String>)

    /**
     * Adds multiple tags to the device, with a callback.
     *
     * @param tags A list of tags to add.
     * @param callback The callback handling the tags addition result.
     */
    public fun addTags(tags: List<String>, callback: NotificareCallback<Unit>)

    /**
     * Removes a specific tag from the device.
     *
     * @param tag The tag to remove.
     */
    public suspend fun removeTag(tag: String)

    /**
     * Removes a specific tag from the device, with a callback.
     *
     * @param tag The tag to remove.
     * @param callback The callback handling the tag removal result.
     */
    public fun removeTag(tag: String, callback: NotificareCallback<Unit>)

    /**
     * Removes multiple tags from the device.
     *
     * @param tags A list of tags to remove.
     */
    public suspend fun removeTags(tags: List<String>)

    /**
     * Removes multiple tags from the device, with a callback.
     *
     * @param tags A list of tags to remove.
     * @param callback The callback handling the tags removal result.
     */
    public fun removeTags(tags: List<String>, callback: NotificareCallback<Unit>)

    /**
     * Clears all tags from the device.
     */
    public suspend fun clearTags()

    /**
     * Clears all tags from the device, with a callback.
     *
     * @param callback The callback handling the tags clearance result.
     */
    public fun clearTags(callback: NotificareCallback<Unit>)

    /**
     * Fetches the "Do Not Disturb" (DND) settings for the device.
     *
     * @return The current DND settings, or `null` if none are set.
     *
     * @see [NotificareDoNotDisturb]
     */
    public suspend fun fetchDoNotDisturb(): NotificareDoNotDisturb?

    /**
     * Fetches the "Do Not Disturb" (DND) settings for the device, with a callback.
     *
     * @param callback The callback handling the DND retrieval result.
     *
     * @see [NotificareDoNotDisturb]
     */
    public fun fetchDoNotDisturb(callback: NotificareCallback<NotificareDoNotDisturb?>)

    /**
     * Updates the "Do Not Disturb" (DND) settings for the device.
     *
     * @param dnd The new DND settings to apply.
     *
     * @see [NotificareDoNotDisturb]
     */
    public suspend fun updateDoNotDisturb(dnd: NotificareDoNotDisturb)

    /**
     * Updates the "Do Not Disturb" (DND) settings for the device, with a callback.
     *
     * @param dnd The new DND settings to apply.
     * @param callback The callback handling the DND update result.
     *
     * @see [NotificareDoNotDisturb]
     */
    public fun updateDoNotDisturb(dnd: NotificareDoNotDisturb, callback: NotificareCallback<Unit>)

    /**
     * Clears the "Do Not Disturb" (DND) settings for the device.
     */
    public suspend fun clearDoNotDisturb()

    /**
     * Clears the "Do Not Disturb" (DND) settings for the device, with a callback.
     *
     * @param callback The callback handling the DND clearance result.
     */
    public fun clearDoNotDisturb(callback: NotificareCallback<Unit>)

    /**
     * Fetches the user data associated with the device.
     *
     * @return The current user data.
     */
    public suspend fun fetchUserData(): NotificareUserData

    /**
     * Fetches the user data associated with the device, with a callback.
     *
     * @param callback The callback handling the user data retrieval result.
     */
    public fun fetchUserData(callback: NotificareCallback<NotificareUserData>)

    /**
     * Updates the custom user data associated with the device.
     *
     * @param userData The updated user data to associate with the device.
     */
    public suspend fun updateUserData(userData: Map<String, String?>)

    /**
     * Updates the custom user data associated with the device, with a callback.
     *
     * @param userData The updated user data to associate with the device.
     * @param callback The callback handling the user data update result.
     */
    public fun updateUserData(userData: Map<String, String?>, callback: NotificareCallback<Unit>)
}
