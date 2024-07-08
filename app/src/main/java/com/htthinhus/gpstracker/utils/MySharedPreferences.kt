package com.htthinhus.gpstracker.utils

import android.content.Context
import android.content.SharedPreferences
import com.mapbox.maps.Style

class MySharedPreferences(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("MySharedPreferences", Context.MODE_PRIVATE)
    private val keyFuelConsumption100km = "fuelConsumption100km"
    private val keyMapStyle = "mapStyle"
    private val keyDeviceId = "deviceId"

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
        return if (getMapStyle() == Style.OUTDOORS) {
            sharedPreferences.edit().putString(keyMapStyle, Style.SATELLITE_STREETS).apply()
            sharedPreferences.getString(keyMapStyle, Style.SATELLITE_STREETS)!!
        } else {
            sharedPreferences.edit().putString(keyMapStyle, Style.OUTDOORS).apply()
            sharedPreferences.getString(keyMapStyle, Style.OUTDOORS)!!
        }
    }

    fun getDeviceId(): String? {
        return sharedPreferences.getString(keyDeviceId, null)
    }

    fun setDeviceId(deviceId: String?) {
        sharedPreferences.edit().putString(keyDeviceId, deviceId).apply()
    }

}