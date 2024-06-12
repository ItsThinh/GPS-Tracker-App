package com.htthinhus.gpstracker

import android.animation.ValueAnimator
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.scale
import com.htthinhus.gpstracker.databinding.FragmentInteractiveMapBinding
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.OnMapClickListener

class InteractiveMapFragment : Fragment(), OnMapClickListener {

    private var _binding: FragmentInteractiveMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var mapboxMap: MapboxMap
    private var animator: ValueAnimator? = null
    private val currentPoint = Point.fromLngLat(107.080641, 10.344681)

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
        }

        initializeMarker()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onMapClick(point: Point): Boolean {
        return true
    }

    private fun initializeMarker() {
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.red_marker)
        val annotationApi = binding.mapView.annotations
        val pointAnnotationManager = annotationApi.createPointAnnotationManager()
        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(currentPoint)
            .withIconImage(
                bitmap.scale(50, 70)
            )
        val myPointAnnotation = pointAnnotationManager.create(pointAnnotationOptions)

    }

}