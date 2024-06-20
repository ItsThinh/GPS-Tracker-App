package com.htthinhus.gpstracker

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore

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

    }
}