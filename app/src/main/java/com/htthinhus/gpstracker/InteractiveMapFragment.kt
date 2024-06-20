package com.htthinhus.gpstracker

import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.graphics.scale
import androidx.navigation.fragment.findNavController
import com.google.firebase.Timestamp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.type.Date
import com.htthinhus.gpstracker.databinding.FragmentInteractiveMapBinding
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.logo.logo
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone
import kotlin.time.Duration.Companion.seconds

class InteractiveMapFragment : Fragment(), OnMapClickListener {

    private var firstTimeOpenFragment = true

    private val DEVICE_ID = "cf509abf-e231-43e0-a117-8b22bd25c7ed"

    private lateinit var binding: FragmentInteractiveMapBinding

    private lateinit var mapboxMap: MapboxMap
    private var animator: ValueAnimator? = null
    private val currentPoint = Point.fromLngLat(107.080641, 10.344681)
    private var isMarkerInitialized = false
    private lateinit var myGPSAnnotation: PointAnnotation
    private lateinit var pointAnnotationManager: PointAnnotationManager
    private var vehicleState: VehicleState? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentInteractiveMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapboxMap = binding.mapView.mapboxMap

        mapboxMap.loadStyle(
            style(Style.OUTDOORS){
            }
        ) {
            binding.mapView.gestures.pitchEnabled = false
            binding.mapView.compass.enabled = false
            binding.mapView.logo.enabled = false
            binding.mapView.attribution.enabled = false
        }
        getRealtimeLocation()
        getVehicleStatus()
        binding.cvVehicleStatus.setOnClickListener {
            changeLockState()
        }

        testGetFirestoreData()

        binding.btnVehicleSessions.setOnClickListener {
            findNavController().navigate(R.id.action_interactiveMapFragment_to_vehicleSessionsFragment)
        }

        binding.btnSetFuel.setOnClickListener {
            showUpdateFuelValueDialog()
        }
    }

    private fun showUpdateFuelValueDialog() {
        val editText = EditText(context)
        editText.inputType = android.text.InputType.TYPE_CLASS_NUMBER

        AlertDialog.Builder(context)
            .setTitle("Update fuel value")
            .setMessage("Enter fuel consumption per 100 km")
            .setView(editText)
            .setPositiveButton("Enter") { dialog, which ->
                val newFuelValue = editText.text.toString().toIntOrNull()
                if (newFuelValue != null) {
                    updateFuelValue(newFuelValue)
                } else {
                    editText.error = "Please enter a valid number"
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateFuelValue(newFuelValue: Int) {
        val mySharedPreferences = MySharedPreferences(requireContext())

        val firestoreRef = FirebaseFirestore.getInstance()
            .collection("devices")
            .document("cf509abf-e231-43e0-a117-8b22bd25c7ed")
            .update("fuelConsumption100km", newFuelValue.toLong())
            .addOnSuccessListener {
                mySharedPreferences.setFuelConsumption100km(newFuelValue)
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


//        docRef.get()
//            .addOnSuccessListener { documents ->
//                if (documents != null) {
//                    for (document in documents) {
//                        Log.d("QUERY_TEST", "${document.id} => ${document.data}")
//                    }
//                } else {
//                    Log.d("DOC_NAMES", "no such document")
//                }
//            }
//            .addOnFailureListener { exception ->
//                Log.d("DOC_NAMES", "get fail with $exception")
//            }

//        val usersRef = FirebaseFirestore.getInstance().collection("user")
//        val query = usersRef.whereEqualTo("isActive", true)
//        query.get()
//            .addOnSuccessListener { documents ->
//                if (documents.isEmpty) {
//                    Log.d("QUERY_TEST", "No matching documents found.")
//                } else {
//                    for (document in documents) {
//                        Log.d("QUERY_TEST", "${document.id} => ${document.data}")
//                    }
//                }
//            }
//            .addOnFailureListener { exception ->
//                Log.w("QUERY_TEST", "Error getting documents: ", exception)
//            }
    }

    override fun onMapClick(point: Point): Boolean {
        return true
    }

    private fun getRealtimeLocation() {
        val firebaseRef = FirebaseDatabase.getInstance().getReference(DEVICE_ID).child("location")
        firebaseRef.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val firebaseLatLng = snapshot.getValue(RealtimeLatLng::class.java)
                    val currentPoint = Point.fromLngLat(
                        firebaseLatLng!!.longitude,
                        firebaseLatLng.latitude
                    )
                    if (isMarkerInitialized) {
                        updateMarker(currentPoint)
                    } else {
                        initializeMarker(currentPoint)
                        isMarkerInitialized = true
                    }

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
        val firebaseRef = FirebaseDatabase.getInstance().getReference(DEVICE_ID).child("vehicleState")
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

    private fun changeLockState() {
        val firebaseRef = FirebaseDatabase.getInstance().getReference(DEVICE_ID).child("vehicleState").child("locked")
        if (vehicleState != null) {
            firebaseRef.setValue(!vehicleState!!.locked)
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
        pointAnnotationManager = annotationApi.createPointAnnotationManager()
        myGPSAnnotation = pointAnnotationManager.create(pointAnnotationOptions)
    }

    private fun updateMarker(currentPoint: Point) {

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
            duration = 3000
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