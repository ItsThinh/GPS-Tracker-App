package com.htthinhus.gpstracker.services

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.htthinhus.gpstracker.R
import com.htthinhus.gpstracker.utils.MySharedPreferences
import com.htthinhus.gpstracker.viewmodels.UserViewModel

class MyFirebaseMessagingService: FirebaseMessagingService() {

    private lateinit var mySharedPreferences: MySharedPreferences

    override fun onCreate() {
        super.onCreate()
        mySharedPreferences = MySharedPreferences(applicationContext)
    }
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        mySharedPreferences.setToken(token)
    }
    
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val bitmapLargeIcon: Bitmap
        val CHANNEL_ID = "CHANNEL_1"
        var contextTitle = "VEHICLE STATUS"
        var contentText: String
        val content:String = message.data["content"]!!

        if (content == "turnOn") {
            contentText = "VEHICLE IS TURNED ON"
            bitmapLargeIcon = BitmapFactory.decodeResource(resources, R.drawable.vehicle_state_on)
        } else if (content == "turnOff") {
            contentText = "VEHICLE IS TURNED OFF"
            bitmapLargeIcon = BitmapFactory.decodeResource(resources, R.drawable.vehicle_state_off)
        } else if (content == "unauthorizedUnlock") {
            contextTitle = "WARNING"
            contentText = "UNAUTHORIZED VEHICLE UNLOCKING"
            bitmapLargeIcon = BitmapFactory.decodeResource(resources, R.drawable.ic_arlet)
        } else {
            contextTitle = "WARNING"
            contentText = "MAIN POWER DISCONNECTED"
            bitmapLargeIcon = BitmapFactory.decodeResource(resources, R.drawable.ic_arlet)
        }

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_vehicle_status)
            .setContentTitle(contextTitle)
            .setContentText(contentText)
            .setLargeIcon(bitmapLargeIcon)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = NotificationManagerCompat.from(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notificationManager.notify(1, notificationBuilder.build())
    }
}