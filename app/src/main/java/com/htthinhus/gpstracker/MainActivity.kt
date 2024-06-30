package com.htthinhus.gpstracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val mySharedPreferences = MySharedPreferences(this)
        if (mySharedPreferences.getFuelConsumption100km() == 0) {
            val firestoreRef = FirebaseFirestore.getInstance()
                .collection("devices")
                .document("cf509abf-e231-43e0-a117-8b22bd25c7ed")
                .get()
                .addOnSuccessListener {
                    if (it.exists()) {
                        mySharedPreferences.setFuelConsumption100km(it.getLong("fuelConsumption100km")?.toInt() ?: 0)
                    }
                }
                .addOnFailureListener {
                    Log.d("FUEL", it.toString())
                }
        }

        val USER_UID = "naw7Ba6apLNIstW0sm491NGXx3G2"
        val firestoreRef = FirebaseFirestore.getInstance()
            .collection("users")
            .document(USER_UID)
            .collection("fcmTokens")
            .document("token1234")

        firestoreRef.set(emptyMap<String, Any>())
            .addOnSuccessListener {
                Log.d("SENDING_FCM_TOKEN", "Token sent")
            }
            .addOnFailureListener {
                Log.d("SENDING_FCM_TOKEN", it.toString())
            }

        createNotificationChannel()

        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM_TOKEN", "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            // Log and toast
            Log.d("FCM_TOKEN", token)
            Toast.makeText(this, token, Toast.LENGTH_SHORT).show()
        })
    }

    private fun createNotificationChannel() {
        val name = "Warning Channel"
        val descriptionText = "Notify when vehicle status change"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        val CHANNEL_ID = "CHANNEL_1"
    }
}