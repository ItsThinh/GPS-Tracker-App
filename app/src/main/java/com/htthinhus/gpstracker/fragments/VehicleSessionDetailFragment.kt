package com.htthinhus.gpstracker.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import com.htthinhus.gpstracker.R
import com.htthinhus.gpstracker.activities.MainActivity
import com.htthinhus.gpstracker.databinding.FragmentVehicleSessionDetailBinding
import com.htthinhus.gpstracker.utils.MySharedPreferences
import com.htthinhus.gpstracker.viewmodels.MainActivityViewModel
import com.htthinhus.gpstracker.viewmodels.UserViewModel
import com.mapbox.api.geocoding.v5.GeocodingCriteria
import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mapbox.geojson.Point
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone


class VehicleSessionDetailFragment : Fragment() {

    private lateinit var binding: FragmentVehicleSessionDetailBinding
    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()

    private val args: VehicleSessionDetailFragmentArgs by navArgs()

    private var startTimeSeconds: Long? = null
    private var endTimeSeconds: Long? = null

    private lateinit var pointList: ArrayList<Point>
    private lateinit var timestampList: ArrayList<Long>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentVehicleSessionDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainActivityViewModel.hideBottomNav()

        startTimeSeconds = args.startTime
        endTimeSeconds = args.endTime

        if (startTimeSeconds != null && endTimeSeconds != null) {
            setDetail()
        }

        binding.btnPlayback.setOnClickListener {
            val direction = VehicleSessionDetailFragmentDirections
                .actionVehicleSessionDetailFragmentToPlaybackFragment(
                    startTimeSeconds!!,
                    endTimeSeconds!!
                )
            findNavController().navigate(direction)
        }
    }

    private fun setDetail() {

        pointList = arrayListOf()
        timestampList = arrayListOf()

        FirebaseFirestore.getInstance()
            .collection("devices")
            .document("cf509abf-e231-43e0-a117-8b22bd25c7ed")
            .collection("locations")
            .whereGreaterThanOrEqualTo("timestamp", Timestamp(Date(startTimeSeconds!!*1000)))
            .whereLessThanOrEqualTo("timestamp", Timestamp(Date(endTimeSeconds!!*1000)))
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    for (document in documents) {
                        val geoPoint = document.get("location") as GeoPoint
                        val point = Point.fromLngLat(geoPoint.longitude, geoPoint.latitude)
                        pointList.add(point)

                        val timestamp = document.get("timestamp") as Timestamp
                        val timestampSecond = timestamp.seconds
                        timestampList.add(timestampSecond)
                    }
                }
                Log.d("TEST_POINT_LIST", "Point List: $pointList")

                var distanceDriven: Double = 0.0

                if (pointList.size > 1) {
                    for (i in 0 until pointList.size - 1) {
                        distanceDriven += TurfMeasurement.distance(pointList[i], pointList[i+1], TurfConstants.UNIT_KILOMETRES)
                        Log.d("TEST_DISTANCE", "distanceDriven: $distanceDriven")
                    }
                    val formattedStringDistanceDriven = String.format(Locale.ENGLISH, "%.3f", distanceDriven) + " km"
                    binding.tvDistanceDriven.text = formattedStringDistanceDriven

                    val mySharedPreferences = MySharedPreferences(requireContext())
                    val fuelConsumption100km = mySharedPreferences.getFuelConsumption100km().toDouble()
                    val fuelConsumptionSession = fuelConsumption100km/100*distanceDriven
                    val formattedfuelConsumptionSession=String.format(Locale.ENGLISH, "%.4f", fuelConsumptionSession) + " L"
                    binding.tvFuelConsumption.text = formattedfuelConsumptionSession

                    val formattedStringFirstLocation = "${pointList[0].latitude()}, ${pointList[0].longitude()}"
                    val formattedStringLastLocation = "${pointList[pointList.size - 1].latitude()}, ${pointList[pointList.size - 1].longitude()}"
                    binding.tvFirstLocation.text = formattedStringFirstLocation
                    binding.tvLastLocation.text = formattedStringLastLocation


                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    dateFormat.timeZone = TimeZone.getTimeZone("GMT+7")
                    val startDateTime = timestampList[0]
                    val endDateTime = timestampList[timestampList.size - 1]
                    val startTime = dateFormat.format(Date(startDateTime*1000))
                    val endTime = dateFormat.format(Date(endDateTime*1000))

                    val drivingTimeSeconds = startDateTime - endDateTime

                    binding.tvStartTime.text = startTime
                    binding.tvEndTime.text = endTime
                    binding.tvDrivingTime.text = formatSecondsToHHMMSS(drivingTimeSeconds)

                    setPlaces()
                }
            }
    }

    private fun formatSecondsToHHMMSS(seconds: Long): String {
        val hour = seconds / 3600
        val minute = (seconds % 3600) / 60
        val second = seconds % 60
        return String.format("%02d:%02d:%02d", hour, minute, second)
    }

    // Reverse geocoding from coordinate to place
    private fun setPlaces() {
        val reverseGeocodeStart = MapboxGeocoding.builder()
            .accessToken(getString(R.string.mapbox_downloads_token))
            .query(Point.fromLngLat(pointList[0].longitude(), pointList[0].latitude()))
            .geocodingTypes(GeocodingCriteria.TYPE_LOCALITY)
            .build()
        reverseGeocodeStart.enqueueCall(object : Callback<GeocodingResponse> {
            override fun onResponse(call: Call<GeocodingResponse>, response: Response<GeocodingResponse>) {
                val results = response.body()!!.features()
                if (results.size > 0) {
                    val firstResultPoint = results[0].placeName()
                    Log.d("GEOCODER_TEST", "onResponse: " + firstResultPoint!!.toString())
                    binding.tvStartAddress.text = if (firstResultPoint != null) firstResultPoint else "N/A"
                } else {
                    Log.d("GEOCODER_TEST", "onResponse: No result found")
                }
            }
            override fun onFailure(call: Call<GeocodingResponse>, throwable: Throwable) {
                throwable.printStackTrace()
            }
        })

        val reverseGeocodeEnd = MapboxGeocoding.builder()
            .accessToken(getString(R.string.mapbox_downloads_token))
            .query(Point.fromLngLat(pointList[0].longitude(), pointList[0].latitude()))
            .geocodingTypes(GeocodingCriteria.TYPE_LOCALITY)
            .build()
        reverseGeocodeEnd.enqueueCall(object : Callback<GeocodingResponse> {
            override fun onResponse(call: Call<GeocodingResponse>, response: Response<GeocodingResponse>) {
                val results = response.body()!!.features()
                if (results.size > 0) {
                    val firstResultPoint:String? = results[0].placeName()
                    Log.d("GEOCODER_TEST", "onResponse: " + firstResultPoint!!.toString())
                    binding.tvEndAddress.text = if (firstResultPoint != null) firstResultPoint else "N/A"
                } else {
                    Log.d("GEOCODER_TEST", "onResponse: No result found")
                }
            }
            override fun onFailure(call: Call<GeocodingResponse>, throwable: Throwable) {
                throwable.printStackTrace()
            }
        })
    }

}