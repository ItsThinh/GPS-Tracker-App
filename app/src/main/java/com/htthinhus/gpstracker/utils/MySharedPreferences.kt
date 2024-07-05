package com.htthinhus.gpstracker.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.mapbox.maps.Style

class MySharedPreferences(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("MySharedPreferences", Context.MODE_PRIVATE)
    private val keyFuelConsumption100km = "fuelConsumption100km"
    private val keyMapStyle = "mapStyle"

    fun getFuelConsumption100km(): Int {
        return sharedPreferences.getInt(keyFuelConsumption100km, -1)
    }

    fun setFuelConsumption100km(value: Int) {
        sharedPreferences.edit().putInt(keyFuelConsumption100km, value).apply()
    }

    fun getMapStyle(): String {
        return sharedPreferences.getString(keyMapStyle, Style.OUTDOORS)!!
    }

    fun setNewMapStyle(): String {
        if (getMapStyle() == Style.OUTDOORS) {
            sharedPreferences.edit().putString(keyMapStyle, Style.SATELLITE_STREETS).apply()
            return sharedPreferences.getString(keyMapStyle, Style.SATELLITE_STREETS)!!
        } else {
            sharedPreferences.edit().putString(keyMapStyle, Style.OUTDOORS).apply()
            return sharedPreferences.getString(keyMapStyle, Style.OUTDOORS)!!
        }
    }

}