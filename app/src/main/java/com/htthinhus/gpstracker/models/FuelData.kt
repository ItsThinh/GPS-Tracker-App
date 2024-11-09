package com.htthinhus.gpstracker.models

data class FuelData(
    val currentFuelLevel: Float = 0.0f,
    val fuelConsumptionPer100km: Float = 0.0f,
    val tankCapacity: Float = 0.0f,
    val warningFuelPercentage: Float = 0.0f

)
