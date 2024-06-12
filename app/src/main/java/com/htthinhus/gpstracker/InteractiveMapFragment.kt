package com.htthinhus.gpstracker

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.htthinhus.gpstracker.databinding.FragmentInteractiveMapBinding
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.style

class InteractiveMapFragment : Fragment() {

    private var _binding: FragmentInteractiveMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var mapboxMap: MapboxMap

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
                .center(Point.fromLngLat(107.080641, 10.344681))
                .zoom(15.0)
                .build()
            mapboxMap.setCamera(cameraOptions)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}