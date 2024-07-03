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
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.bottomnavigation.BottomNavigationView
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

        getFuel()

        setupBottomNavigationMenu()

        createNotificationChannel()
    }

    private fun setupBottomNavigationMenu() {
        val bottomNavigationView =
            findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        val navController = navHostFragment.navController
        bottomNavigationView.setupWithNavController(navController)
    }

    private fun getFuel() {
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