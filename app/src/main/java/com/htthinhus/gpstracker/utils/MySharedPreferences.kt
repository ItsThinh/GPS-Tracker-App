package com.htthinhus.gpstracker.utils

import android.content.Context
import android.content.SharedPreferences
import com.mapbox.maps.Style

class MySharedPreferences(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("MySharedPreferences", Context.MODE_PRIVATE)

    private val keyFuelConsumption100km = "fuelConsumption100km"
    private val keyCurrentFuelLevel = "currentFuelLevel"
    private val keyTankCapacity = "tankCapacity"
    private val keyWarningFuelPercentage = "warningFuelPercentage"
    private val keyMapStyle = "mapStyle"
    private val keyDeviceId = "deviceId"
    private val keyToken = "token"

    fun getFuelConsumption100km(): Float {
        return sharedPreferences.getFloat(keyFuelConsumption100km, 0f)
    }

    fun setFuelConsumption100km(value: Float) {
        sharedPreferences.edit().putFloat(keyFuelConsumption100km, value).apply()
    }

    fun getCurrentFuelLevel(): Float {
        return sharedPreferences.getFloat(keyCurrentFuelLevel, 0f)
    }

    fun setCurrentFuelLevel(value: Float) {
        sharedPreferences.edit().putFloat(keyCurrentFuelLevel, value).apply()
    }

    fun getTankCapacity(): Float {
        return sharedPreferences.getFloat(keyTankCapacity, 0f)
    }

    fun setTankCapacity(value: Float) {
        sharedPreferences.edit().putFloat(keyTankCapacity, value).apply()
    }

    fun getWarningFuelPercentage(): Float {
        return sharedPreferences.getFloat(keyWarningFuelPercentage, 0f)
    }

    fun setWarningFuelPercentage(value: Float) {
        sharedPreferences.edit().putFloat(keyWarningFuelPercentage, value).apply()
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

    fun setToken(token: String) {
        sharedPreferences.edit().putString(keyToken, token).apply()
    }

    fun getToken(): String? {
        return sharedPreferences.getString(keyToken, null)
    }

}