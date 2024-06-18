package com.htthinhus.gpstracker

import com.google.firebase.Timestamp

data class VehicleSession(
    val startTime: Timestamp? = null,
    val endTime: Timestamp? = null
)
