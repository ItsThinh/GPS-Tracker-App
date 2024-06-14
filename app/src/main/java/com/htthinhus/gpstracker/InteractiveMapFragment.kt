package com.htthinhus.gpstracker

import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.graphics.scale
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.htthinhus.gpstracker.databinding.FragmentInteractiveMapBinding
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.style
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

class InteractiveMapFragment : Fragment(), OnMapClickListener {

    private val DEVICE_ID = "cf509abf-e231-43e0-a117-8b22bd25c7ed"

    private var _binding: FragmentInteractiveMapBinding? = null
    private val binding get() = _binding!!

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
        _binding = FragmentInteractiveMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapboxMap = binding.mapView.mapboxMap

        mapboxMap.loadStyle(
            style(Style.OUTDOORS){
            }
        ) {
            val cameraOptions = CameraOptions.Builder()
                .center(currentPoint)
                .zoom(15.0)
                .build()
            mapboxMap.setCamera(cameraOptions)

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
                    Toast.makeText(context, currentPoint.toString(), Toast.LENGTH_LONG).show()
                    if (isMarkerInitialized) {
                        updateMarker(currentPoint)
                    } else {
                        initializeMarker(currentPoint)
                        isMarkerInitialized = true
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
            duration = 1000
            start()
        }

        myGPSAnnotation.point = currentPoint

//        myGPSAnnotation.point = currentPoint
//        pointAnnotationManager.update(myGPSAnnotation)
    }

}