package com.github.im.group.notification

import android.content.Context
import com.google.firebase.FirebaseApp
import io.github.aakira.napier.Napier

object FirebaseBootstrap {

    /**
     * Ensure Firebase is available before any FCM API is touched.
     *
     * In local/dev builds we may intentionally run without `google-services.json`.
     * In that case Firebase returns null for the default app and we should degrade
     * gracefully instead of crashing the whole application during startup.
     */
    fun ensureInitialized(context: Context): FirebaseApp? {
        FirebaseApp.getApps(context).firstOrNull()?.let { existingApp ->
            return existingApp
        }

        val initializedApp = FirebaseApp.initializeApp(context)
        if (initializedApp == null) {
            Napier.w(
                "Firebase is not configured for this build. " +
                    "Skipping FCM initialization until google-services.json is provided."
            )
        }
        return initializedApp
    }
}
