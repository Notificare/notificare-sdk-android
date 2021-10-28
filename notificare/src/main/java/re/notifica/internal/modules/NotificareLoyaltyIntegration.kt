package re.notifica.internal.modules

import re.notifica.InternalNotificareApi
import re.notifica.NotificareCallback
import re.notifica.models.NotificareNotification

@InternalNotificareApi
public interface NotificareLoyaltyIntegration {

    public fun handlePresentationDecision(
        notification: NotificareNotification,
        callback: PresentationDecisionCallback,
    )

    public fun handleStorageUpdate(
        notification: NotificareNotification,
        includeInWallet: Boolean,
        callback: NotificareCallback<Unit>,
    )


    public interface PresentationDecisionCallback {
        public fun presentGooglePass(url: String)

        public fun presentPKPass(includedInWallet: Boolean)

        public fun onFailure(e: Exception)
    }
}
