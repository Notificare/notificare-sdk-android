package re.notifica.callbacks

import re.notifica.NotificareCallback
import re.notifica.internal.common.runBlockingNotificare
import re.notifica.models.NotificareDoNotDisturb
import re.notifica.models.NotificareUserData
import re.notifica.modules.NotificareDeviceManager

fun NotificareDeviceManager.register(
    userId: String?,
    userName: String?,
    callback: NotificareCallback<Unit>,
) = runBlockingNotificare(callback) { register(userId, userName) }

fun NotificareDeviceManager.updatePreferredLanguage(
    preferredLanguage: String?,
    callback: NotificareCallback<Unit>,
) = runBlockingNotificare(callback) { updatePreferredLanguage(preferredLanguage) }

// region Tags

fun NotificareDeviceManager.fetchTags(
    callback: NotificareCallback<List<String>>,
) = runBlockingNotificare(callback) { fetchTags() }

fun NotificareDeviceManager.addTag(
    tag: String,
    callback: NotificareCallback<Unit>,
) = runBlockingNotificare(callback) { addTag(tag) }

fun NotificareDeviceManager.addTags(
    tags: List<String>,
    callback: NotificareCallback<Unit>,
) = runBlockingNotificare(callback) { addTags(tags) }

fun NotificareDeviceManager.removeTag(
    tag: String,
    callback: NotificareCallback<Unit>,
) = runBlockingNotificare(callback) { removeTag(tag) }

fun NotificareDeviceManager.removeTags(
    tags: List<String>,
    callback: NotificareCallback<Unit>,
) = runBlockingNotificare(callback) { removeTags(tags) }

fun NotificareDeviceManager.clearTags(
    callback: NotificareCallback<Unit>,
) = runBlockingNotificare(callback) { clearTags() }

// endregion

// region Do not disturb

fun NotificareDeviceManager.fetchDoNotDisturb(
    callback: NotificareCallback<NotificareDoNotDisturb?>,
) = runBlockingNotificare(callback) { fetchDoNotDisturb() }

fun NotificareDeviceManager.updateDoNotDisturb(
    dnd: NotificareDoNotDisturb,
    callback: NotificareCallback<Unit>,
) = runBlockingNotificare(callback) { updateDoNotDisturb(dnd) }

fun NotificareDeviceManager.clearDoNotDisturb(
    callback: NotificareCallback<Unit>,
) = runBlockingNotificare(callback) { clearDoNotDisturb() }

// endregion

// region User data

fun NotificareDeviceManager.fetchUserData(
    callback: NotificareCallback<NotificareUserData?>,
) = runBlockingNotificare(callback) { fetchUserData() }

fun NotificareDeviceManager.updateUserData(
    userData: NotificareUserData,
    callback: NotificareCallback<Unit>,
) = runBlockingNotificare(callback) { updateUserData(userData) }

// endregion
