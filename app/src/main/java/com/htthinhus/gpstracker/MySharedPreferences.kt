package com.htthinhus.gpstracker

import android.content.Context
import android.content.SharedPreferences

class MySharedPreferences(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("MySharedPreferences", Context.MODE_PRIVATE)
    private val keyFuelConsumption100km = "fuelConsumption100km"

    fun getFuelConsumption100km(): Int {
        return sharedPreferences.getInt(keyFuelConsumption100km, 0)
    }

    fun setFuelConsumption100km(value: Int) {
        sharedPreferences.edit().putInt(keyFuelConsumption100km, value).apply()
    }
}