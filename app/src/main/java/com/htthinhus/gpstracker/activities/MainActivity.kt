package com.htthinhus.gpstracker.activities

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.htthinhus.gpstracker.utils.MySharedPreferences
import com.htthinhus.gpstracker.R
import com.htthinhus.gpstracker.viewmodels.UserViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private val userViewModel: UserViewModel by viewModels()

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
        userLoginStateObserve()
    }

    private fun userLoginStateObserve() {
        userViewModel.loginState.observe(this, Observer {
            if (it == true) {
                bottomNavigationView.visibility = View.VISIBLE
            } else {
                bottomNavigationView.visibility = View.GONE
            }
        })
    }

    private fun setupBottomNavigationMenu() {
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        val navController = navHostFragment.navController
        bottomNavigationView.setupWithNavController(navController)
    }

    private fun getFuel() {
        val mySharedPreferences = MySharedPreferences(this)
        if (mySharedPreferences.getFuelConsumption100km() == -1) {
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