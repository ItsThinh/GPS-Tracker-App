package com.htthinhus.gpstracker.fragments

import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.graphics.scale
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.htthinhus.gpstracker.utils.MySharedPreferences
import com.htthinhus.gpstracker.R
import com.htthinhus.gpstracker.models.RealtimeLatLng
import com.htthinhus.gpstracker.models.VehicleState
import com.htthinhus.gpstracker.databinding.FragmentInteractiveMapBinding
import com.htthinhus.gpstracker.models.FuelData
import com.htthinhus.gpstracker.viewmodels.UserViewModel
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.annotation.AnnotationConfig
import com.mapbox.maps.plugin.annotation.AnnotationPlugin
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.OnPolylineAnnotationClickListener
import com.mapbox.maps.plugin.annotation.generated.OnPolylineAnnotationInteractionListener
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotation
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.logo.logo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Random
import java.util.TimeZone

class InteractiveMapFragment : Fragment(), OnMapClickListener {

    private val userViewModel: UserViewModel by activityViewModels()

    private var DEVICE_ID: String? = null

    private var firstTimeOpenFragment = true
    private var isMarkerInitialized = false

    private var vehicleState: VehicleState? = null
    private lateinit var currentPoint: Point

    private lateinit var mySharedPreferences: MySharedPreferences

    private lateinit var binding: FragmentInteractiveMapBinding

    private lateinit var mapboxMap: MapboxMap
    private var animator: ValueAnimator? = null
    private lateinit var myGPSAnnotation: PointAnnotation
    private lateinit var pointAnnotationManager: PointAnnotationManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentInteractiveMapBinding.inflate(inflater, container, false)
        mySharedPreferences = MySharedPreferences(requireContext())
        DEVICE_ID = mySharedPreferences.getDeviceId()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firstTimeOpenFragment = true
        isMarkerInitialized = false

        val navController = findNavController()

        userViewModel.loginState.observe(viewLifecycleOwner, Observer { loginState ->
            if (loginState == false) {
                mySharedPreferences.setDeviceId(null)
                navController.navigate(R.id.loginFragment)
            }
        })

        // setup View visibility
        if(DEVICE_ID != null) {
            setupMap()
        } else {
            binding.mapView.visibility = View.GONE
            binding.btnChangeMapMode.visibility = View.GONE
            binding.btnLaunchGoogleMaps.visibility = View.GONE
            binding.cvVehicleStatus.visibility = View.GONE
        }

        binding.btnChangeMapMode.setOnClickListener {
            binding.mapView.mapboxMap.loadStyle(
                style(mySharedPreferences.setNewMapStyle()){}
            )
        }

        binding.btnLaunchGoogleMaps.setOnClickListener {
            intentToGMap()
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM_TOKEN", "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            } else {
                Log.w("FCM_TOKEN", task.result)
            }
        })

    }

    private fun setupMap() {
        mapboxMap = binding.mapView.mapboxMap

        mapboxMap.loadStyle(
            style(mySharedPreferences.getMapStyle()){
            }
        ) {
            binding.mapView.gestures.pitchEnabled = false
            binding.mapView.compass.enabled = false
            binding.mapView.logo.enabled = false
            binding.mapView.attribution.enabled = false
        }
        getRealtimeLocation()
        getVehicleStatus()
        getFuelData()
        binding.cvVehicleStatus.setOnClickListener {
            changeLockState()
        }
    }

    private fun testGetFirestoreData() {
        val docRef = FirebaseFirestore.getInstance()
            .collection("devices")
            .document("cf509abf-e231-43e0-a117-8b22bd25c7ed")
            .collection("locations")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("GMT+7"))
        dateFormat.timeZone = TimeZone.getTimeZone("GMT+7")
//        val startTimestamp = Timestamp(dateFormat.parse("2024-06-17 03:26:00")).seconds
//        val endTimestamp = Timestamp(dateFormat.parse("2024-06-17 03:27:00")).seconds

        val endTimestamp = Timestamp(java.util.Date(dateFormat.parse("2024-06-17 03:26:00").time))

        val query = docRef
            //.whereGreaterThanOrEqualTo("timestamp", startTimestamp)
            .whereLessThanOrEqualTo("timestamp", endTimestamp)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    Log.d("QUERY_TEST", "${document.id} => ${document.data}")
                }
            }
            .addOnFailureListener { exception ->
                Log.w("QUERY_TEST", "Error getting documents: ", exception)
            }
    }

    private fun intentToGMap() {
        val gMapUri =
            Uri.parse("https://www.google.com/maps/dir/?api=1" +
                    "&destination=${currentPoint.latitude()}" +
                    ",+${currentPoint.longitude()}")

        val gMapIntent = Intent(Intent.ACTION_VIEW, gMapUri)
        gMapIntent.setPackage("com.google.android.apps.maps")
        gMapIntent.resolveActivity(requireActivity().packageManager)?.let {
            startActivity(gMapIntent)
        }
    }

    override fun onMapClick(point: Point): Boolean {
        return true
    }

    private fun getRealtimeLocation() {
        val firebaseRef = FirebaseDatabase.getInstance().getReference(DEVICE_ID!!).child("location")
        firebaseRef.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val data = snapshot.getValue(RealtimeLatLng::class.java)
                    currentPoint = Point.fromLngLat(
                        data!!.longitude,
                        data.latitude
                    )
                    val dataSpeedString = String.format(Locale.US, "%.2f", data.speed) + " kmph"
                    binding.tvSpeed.text = dataSpeedString
                    if (isMarkerInitialized) {
                        updateMarker(currentPoint)
                    } else {
                        initializeMarker(currentPoint)
                        isMarkerInitialized = true
                    }

                    Log.d("FIRST_OPEN_STATE", "firstTimeOpenFragment: $firstTimeOpenFragment")
                    if (firstTimeOpenFragment) {
                        val cameraOptions = CameraOptions.Builder()
                            .center(currentPoint)
                            .zoom(15.0)
                            .build()
                        binding.mapView.mapboxMap.easeTo(
                            cameraOptions,
                            MapAnimationOptions.mapAnimationOptions {
                                duration(0)
                            }
                        )
                        firstTimeOpenFragment = false
                    }

                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun getVehicleStatus() {
        val firebaseRef = FirebaseDatabase.getInstance().getReference(DEVICE_ID!!).child("vehicleState")
        firebaseRef.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    vehicleState = snapshot.getValue(VehicleState::class.java)

                    if (vehicleState != null) {
                        if (vehicleState!!.locked) {
                            binding.ivStatusIcon.setImageResource(R.drawable.vehicle_state_locked)
                            binding.tvVehicleStatus.text = "locked"
                        } else {
                            if (vehicleState!!.status) {
                                binding.ivStatusIcon.setImageResource(R.drawable.vehicle_state_on)
                                binding.tvVehicleStatus.text = "on"
                            } else {
                                binding.ivStatusIcon.setImageResource(R.drawable.vehicle_state_off)
                                binding.tvVehicleStatus.text = "off"
                            }
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun getFuelData() {
        val firebaseRef = FirebaseDatabase.getInstance().getReference(DEVICE_ID!!).child("fuelData")
        firebaseRef.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val data = snapshot.getValue(FuelData::class.java)

                    val fuelDataString = String.format(Locale.US, "%.2f", data!!.currentFuelLevel) + "L"
                    binding.tvCurrentFuelLevel.text = fuelDataString

                    val height = binding.fuelTankFrame.height

                    val percentage = data.currentFuelLevel/data.tankCapacity
                    binding.progressFill.layoutParams.height = (height * percentage).toInt()
                    binding.progressFill.requestLayout()
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun changeLockState() {
        val firebaseRef = FirebaseDatabase.getInstance().getReference(DEVICE_ID!!).child("vehicleState")
        if (vehicleState != null) {
            if (!vehicleState!!.locked){
                val updates = mapOf<String, Boolean>(
                    "locked" to !vehicleState!!.locked,
                    "status" to false
                )
                firebaseRef.updateChildren(updates).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("UPDATE_LOCKED", "Update successful")
                    } else {
                        Log.e("UPDATE_LOCKED", "Update failed", task.exception)
                    }
                }
            } else {
                FirebaseDatabase.getInstance().getReference(DEVICE_ID!!).child("vehicleState")
                    .child("locked").setValue(!vehicleState!!.locked)
            }
        }
    }

    private fun initializeMarker(currentPoint: Point) {
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.red_marker)
        val annotationApi = binding.mapView.annotations
        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(currentPoint)
            .withIconImage(
                bitmap.scale(50, 70)
            )
            .withIconAnchor(IconAnchor.BOTTOM)
        pointAnnotationManager = annotationApi.createPointAnnotationManager()
        myGPSAnnotation = pointAnnotationManager.create(pointAnnotationOptions)
    }

    private fun updateMarker(currentPoint: Point, durationValue: Long = 3000) {

        animator?.let {
            if (it.isStarted) {
                myGPSAnnotation.point = it.animatedValue as Point
                it.cancel()
            }
        }

        val pointEvaluator = TypeEvaluator<Point> { fraction, startValue, endValue ->
            Point.fromLngLat(
                startValue.longitude() + fraction * (endValue.longitude() - startValue.longitude()),
                startValue.latitude() + fraction * (endValue.latitude() - startValue.latitude())
            )
        }

        animator = ValueAnimator().apply {
            setObjectValues(myGPSAnnotation.point, currentPoint)
            setEvaluator(pointEvaluator)
            addUpdateListener {
                myGPSAnnotation.point = it.animatedValue as Point
                pointAnnotationManager.update(myGPSAnnotation)
            }
            duration = durationValue
            start()
        }

        myGPSAnnotation.point = currentPoint

        val cameraOptions = CameraOptions.Builder()
            .center(myGPSAnnotation.point)
            .zoom(15.0)
            .build()
        binding.mapView.mapboxMap.easeTo(
            cameraOptions,
            MapAnimationOptions.mapAnimationOptions {
                duration(3000)
            }
        )

    }

}