package com.htthinhus.gpstracker.services

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.htthinhus.gpstracker.R

class MyFirebaseMessagingService: FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        sendTokenToFirestore(token)
        Log.d("SENDING_FCM_TOKEN", token)
    }

    private fun sendTokenToFirestore(token: String) {

        val USER_UID = "naw7Ba6apLNIstW0sm491NGXx3G2" // change to UID of authentication later
        val firestoreRef = FirebaseFirestore.getInstance()
            .collection("users")
            .document(USER_UID)
            .collection("fcmTokens")
            .document(token)

        firestoreRef.set(emptyMap<String, Any>())
            .addOnSuccessListener {
                Log.d("SENDING_FCM_TOKEN", "Token sent")
            }
            .addOnFailureListener {
                Log.d("SENDING_FCM_TOKEN", it.toString())
            }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val status:Boolean = message.data["status"].toBoolean()

        val CHANNEL_ID = "CHANNEL_1"
        val contentText = if (status) "VEHICLE TURN ON" else "VEHICLE TURN OFF"

        var notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_vehicle_status)
            .setContentTitle("VEHICLE STATUS")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_MAX)

        val notificationManager = NotificationManagerCompat.from(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notificationManager.notify(1, notificationBuilder.build())
    }
}