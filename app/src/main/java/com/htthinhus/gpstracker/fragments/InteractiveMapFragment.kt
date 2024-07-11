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
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.graphics.scale
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.htthinhus.gpstracker.utils.MySharedPreferences
import com.htthinhus.gpstracker.R
import com.htthinhus.gpstracker.models.RealtimeLatLng
import com.htthinhus.gpstracker.models.VehicleState
import com.htthinhus.gpstracker.databinding.FragmentInteractiveMapBinding
import com.htthinhus.gpstracker.viewmodels.UserViewModel
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.getLayer
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

    private val handler: Handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null
    private var isForward = true
    private var currentIndex: Int = 0
    private var isPlaybackRunning = true
    private var previousProgress = 0

    val coordinates: List<Point> = listOf(
        Point.fromLngLat(107.101846, 10.358419),
        Point.fromLngLat(107.10174918157263, 10.357923083757742),
        Point.fromLngLat(107.10119099506574, 10.357268286320686),
        Point.fromLngLat(107.10070807972313, 10.356793840747553),
        Point.fromLngLat(107.10107286013488, 10.356160598047126),
        Point.fromLngLat(107.10138399636841, 10.35588619247975),
        Point.fromLngLat(107.1018238786296, 10.355464029599458),
        Point.fromLngLat(107.10208137068493, 10.355337380624524),
        Point.fromLngLat(107.1022959473977, 10.355611786672215),
    )

    //Polylines
    private var polylineAnnotationManager: PolylineAnnotationManager? = null
    private lateinit var annotationPlugin: AnnotationPlugin
    val points = listOf(
        Point.fromLngLat(107.101846, 10.358419),
        Point.fromLngLat(107.10174918157263, 10.357923083757742),
        Point.fromLngLat(107.10119099506574, 10.357268286320686),
        Point.fromLngLat(107.10070807972313, 10.356793840747553),
        Point.fromLngLat(107.10107286013488, 10.356160598047126),
        Point.fromLngLat(107.10138399636841, 10.35588619247975),
        Point.fromLngLat(107.1018238786296, 10.355464029599458),
        Point.fromLngLat(107.10208137068493, 10.355337380624524),
        Point.fromLngLat(107.1022959473977, 10.355611786672215),
        //add more
        Point.fromLngLat(107.10251052411047, 10.355886192947955),
        Point.fromLngLat(107.10272510082324, 10.356160598995695),
        Point.fromLngLat(107.10293967753601, 10.356435005043435),
        Point.fromLngLat(107.10315425424878, 10.356709411091175),
        Point.fromLngLat(107.10336883096155, 10.356983817138915),
        Point.fromLngLat(107.10358340767432, 10.357258223186655),
        Point.fromLngLat(107.10379798438709, 10.357532629234395),
        Point.fromLngLat(107.10401256109986, 10.357807035282135),
        Point.fromLngLat(107.10422713781263, 10.358081441329875),
        Point.fromLngLat(107.1044417145254, 10.358355847377615),
        Point.fromLngLat(107.10465629123817, 10.358630253425355),
        Point.fromLngLat(107.10487086795094, 10.358904659473096),
        Point.fromLngLat(107.10508544466371, 10.359179065520834),
        Point.fromLngLat(107.10530002137648, 10.359453471568575),
        Point.fromLngLat(107.10551459808925, 10.359727877616315),
        Point.fromLngLat(107.10572917480202, 10.360002283664056),
        Point.fromLngLat(107.10594375151479, 10.360276689711794),
        Point.fromLngLat(107.10615832822756, 10.360551095759535),
        Point.fromLngLat(107.10637290494033, 10.360825501807275),
        Point.fromLngLat(107.1065874816531, 10.361099907855015)
    )


    private var currentPolylineIndex: Int = 0
    private val polylineAnnotations = mutableListOf<PolylineAnnotation>()
    val pointsList = listOf(
        listOf(Point.fromLngLat(107.101846, 10.358419),Point.fromLngLat(107.10174918157263, 10.357923083757742)),
        listOf(Point.fromLngLat(107.10174918157263, 10.357923083757742),Point.fromLngLat(107.10119099506574, 10.357268286320686)),
        listOf(Point.fromLngLat(107.10119099506574, 10.357268286320686), Point.fromLngLat(107.10107286013488, 10.356160598047126)),
        listOf(Point.fromLngLat(107.10107286013488, 10.356160598047126), Point.fromLngLat(107.10138399636841, 10.35588619247975)),
        listOf(Point.fromLngLat(107.10138399636841, 10.35588619247975), Point.fromLngLat(107.1018238786296, 10.355464029599458)),
        listOf(Point.fromLngLat(107.1018238786296, 10.355464029599458), Point.fromLngLat(107.10208137068493, 10.355337380624524)),
        listOf(Point.fromLngLat(107.10208137068493, 10.355337380624524), Point.fromLngLat(107.1022959473977, 10.355611786672215)),
    )

    private val pointList2 = mutableListOf<Point>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentInteractiveMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firstTimeOpenFragment = true
        isMarkerInitialized = false
        mySharedPreferences = MySharedPreferences(requireContext())
        val navController = findNavController()
        val auth = Firebase.auth

        userViewModel.loginState.observe(viewLifecycleOwner, Observer { loginState ->
            if (loginState == false) {
                mySharedPreferences.setDeviceId(null)
                navController.navigate(R.id.loginFragment)
            }
        })

        if(mySharedPreferences.getDeviceId() != null) {
            setupMap()
        }

        binding.btnChangeMapMode.setOnClickListener {
            binding.mapView.mapboxMap.loadStyle(
                style(mySharedPreferences.setNewMapStyle()){}
            )
        }

        binding.btnLaunchGoogleMaps.setOnClickListener {
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


        binding.btnPlayback.setOnClickListener {
            startUpdatingMarker2()
        }
        binding.btnControl.setOnClickListener {
            tooglePlaybackControl()
        }
        binding.btnDrawPolylines.setOnClickListener {
            addPolylines()
        }


        binding.mapView.mapboxMap.loadStyle(style(Style.OUTDOORS) {}) {
            annotationPlugin = binding.mapView.annotations
            polylineAnnotationManager = annotationPlugin.createPolylineAnnotationManager(
                annotationConfig = AnnotationConfig("pitch_outline", "line_source", "line_layer")
            )
        }

        currentPolylineIndex = 1
        pointList2.add(coordinates[0])
        binding.btnAdd.setOnClickListener {
//            addPolyline()
            addPoint()
        }
        binding.btnDelete.setOnClickListener {
//            deletePolyline()
            deleteLastPoint()
        }
    }

    private fun addPoint() {
        if(currentPolylineIndex < coordinates.size) {
            pointList2.add(coordinates[currentPolylineIndex])
            currentPolylineIndex++
            updatePolyline()
        } else {
            Toast.makeText(context, "no more point to add", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteLastPoint() {
        if(pointList2.isNotEmpty()) {
            pointList2.removeAt(pointList2.size - 1)
            currentPolylineIndex--
            updatePolyline()
        }
    }

    private fun deleteLastPolyline() {
        if (polylineAnnotations.isNotEmpty()) {
            val lastPolyline = polylineAnnotations.removeAt(polylineAnnotations.size - 1)
            polylineAnnotationManager?.delete(lastPolyline)
            pointList2.removeAt(pointList2.size - 1)
            currentPolylineIndex--
        }
    }

    private fun updatePolyline() {
        if(pointList2.size > 1){
            val polylineAnnotationOptions = PolylineAnnotationOptions()
                .withPoints(pointList2)
                .withLineColor(Color.BLACK)
                .withLineWidth(5.0)

            val polylineAnnotation = polylineAnnotationManager?.create(polylineAnnotationOptions)
            polylineAnnotation?.let {
                polylineAnnotations.add(it)
            }
        }
    }

    private fun addPolyline() {
        if (currentPolylineIndex < pointsList.size) {
            val points = pointsList[currentPolylineIndex]
            val polylineAnnotationOptions: PolylineAnnotationOptions = PolylineAnnotationOptions()
                .withPoints(points)
                .withLineColor(Color.RED)
                .withLineWidth(8.0)

            val polylineAnnotation = polylineAnnotationManager?.create(polylineAnnotationOptions)
            polylineAnnotation?.let {
                polylineAnnotations.add(it)
                currentPolylineIndex++
            }
        } else {
            Toast.makeText(context, "No more polylines to add", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deletePolyline() {
        if (polylineAnnotations.isNotEmpty()) {
            val polylineAnnotation = polylineAnnotations.removeAt(polylineAnnotations.size - 1)
            polylineAnnotationManager?.delete(polylineAnnotation)
            currentPolylineIndex--
        } else {
            Toast.makeText(context, "No polylines to delete", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addPolylines() {

        binding.mapView.mapboxMap.loadStyle(style(Style.OUTDOORS) {}) {
            annotationPlugin = binding.mapView.annotations
            polylineAnnotationManager = annotationPlugin.createPolylineAnnotationManager(
                annotationConfig = AnnotationConfig("pitch_outline", "line_source", "line_layer")
            ).apply {
                it.getLayer(LAYER_ID)?.let { layer ->
                    Toast.makeText(context, layer.layerId, Toast.LENGTH_SHORT).show()
                }
                addClickListener(
                    OnPolylineAnnotationClickListener {
                        Toast.makeText(context, "click ${it.id}", Toast.LENGTH_SHORT).show()
                        false
                    }
                )

                addInteractionListener(object: OnPolylineAnnotationInteractionListener{
                    override fun onSelectAnnotation(annotation: PolylineAnnotation) {
                        Toast.makeText(context, "onSelectAnnotation ${annotation.id}", Toast.LENGTH_SHORT).show()
                    }

                    override fun onDeselectAnnotation(annotation: PolylineAnnotation) {
                        Toast.makeText(context, "onDeselectAnnotation ${annotation.id}", Toast.LENGTH_SHORT).show()
                    }
                })

                val polylineAnnotationOptions = PolylineAnnotationOptions()
                    .withPoints(points)
                    .withLineColor(Color.RED)
                    .withLineWidth(5.0)
                create(polylineAnnotationOptions)

            }
        }
    }

    private fun startUpdatingMarker() {

        binding.seekBar.max = coordinates.size - 1

        runnable = object: Runnable {
            override fun run() {


                animator?.let {
                    if (it.isStarted) {
                        myGPSAnnotation.point = it.animatedValue as Point
                        it.cancel()
                    }
                }

                // Determine next index based on direction
                if (isForward) {
                    currentIndex++
                    if (currentIndex >= coordinates.size) {
                        currentIndex = 0 // Loop back to the beginning
                    }
                } else {
                    currentIndex--
                    if (currentIndex < 0) {
                        currentIndex = coordinates.size - 1 // Loop to the end
                    }
                }

                val pointEvaluator = TypeEvaluator<Point> { fraction, startValue, endValue ->
                    Point.fromLngLat(
                        startValue.longitude() + fraction * (endValue.longitude() - startValue.longitude()),
                        startValue.latitude() + fraction * (endValue.latitude() - startValue.latitude())
                    )
                }

                animator = ValueAnimator().apply {
                    setObjectValues(myGPSAnnotation.point, coordinates[currentIndex])
                    setEvaluator(pointEvaluator)
                    addUpdateListener {
                        myGPSAnnotation.point = it.animatedValue as Point
                        pointAnnotationManager.update(myGPSAnnotation)
                    }
                    duration = 200
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
                        duration(500)
                    }
                )

                binding.seekBar.progress = currentIndex

                handler.postDelayed(this, 3000)

            }

        }

        handler.post(runnable!!)
    }

    private fun startUpdatingMarker2() {
        val seekBar = binding.seekBar
        seekBar.max = coordinates.size - 1
        seekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
//                if (fromUser) {
                    currentIndex = progress
                    updateMarker(coordinates[currentIndex], 0)
                    if (progress > previousProgress) {
                        addPoint()
                        Toast.makeText(context, "Add called", Toast.LENGTH_SHORT).show()
                    } else {
                        deleteLastPoint()
                        Toast.makeText(context, "Delete called", Toast.LENGTH_SHORT).show()
                    }
                    previousProgress = progress
//                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                handler.removeCallbacks(runnable!!)
                isPlaybackRunning = false
                binding.btnControl.text = "Remuse"
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
//                startUpdatingMarkerFollow2()
            }

        })
        startUpdatingMarkerFollow2()
    }
    private fun tooglePlaybackControl() {
        if (isPlaybackRunning) {
            handler.removeCallbacks(runnable!!)
            binding.btnControl.text = "Resume"
        } else {
            handler.post(runnable!!)
            binding.btnControl.text = "Pause"
        }
        isPlaybackRunning = !isPlaybackRunning
    }

    private fun startUpdatingMarkerFollow2() {
        runnable = object: Runnable{
            override fun run() {
                currentIndex++
                if (currentIndex >= coordinates.size) {
                    currentIndex = 0
                }

                updateMarker(coordinates[currentIndex], 0)
                binding.seekBar.progress = currentIndex

                handler.postDelayed(this, 3000)
            }
        }
        handler.post(runnable!!)
    }

    private fun setupPlaybackFunctionTest() {

        binding.seekBar.max = coordinates.size - 1

        binding.seekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateMarker(coordinates[progress], 250)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

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
        binding.cvVehicleStatus.setOnClickListener {
            changeLockState()
        }
    }

//    private fun showUpdateFuelValueDialog() {
//        val editText = EditText(context)
//        editText.inputType = android.text.InputType.TYPE_CLASS_NUMBER
//
//        AlertDialog.Builder(context)
//            .setTitle("Update fuel value")
//            .setMessage("Enter fuel consumption per 100 km")
//            .setView(editText)
//            .setPositiveButton("Enter") { dialog, which ->
//                val newFuelValue = editText.text.toString().toIntOrNull()
//                if (newFuelValue != null) {
////                    updateFuelValue(newFuelValue)
//                } else {
//                    editText.error = "Please enter a valid number"
//                }
//            }
//            .setNegativeButton("Cancel", null)
//            .show()
//    }

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
                    currentPoint = Point.fromLngLat(
                        firebaseLatLng!!.longitude,
                        firebaseLatLng.latitude
                    )
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

    companion object {
        private const val DEVICE_ID = "cf509abf-e231-43e0-a117-8b22bd25c7ed"
        private const val LAYER_ID = "line_layer"
        private const val SOURCE_ID = "line_source"
        private const val PITCH_OUTLINE = "pitch-outline"
    }

}