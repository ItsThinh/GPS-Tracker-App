package com.htthinhus.gpstracker.models

import com.google.firebase.Timestamp

data class VehicleSession(
    val startTime: Timestamp? = null,
    val endTime: Timestamp? = null
)
