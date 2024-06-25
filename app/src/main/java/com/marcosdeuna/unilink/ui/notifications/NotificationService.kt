package com.marcosdeuna.unilink.ui.notifications

import android.annotation.SuppressLint
import com.google.firebase.messaging.FirebaseMessagingService

class NotificationService: FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }
}