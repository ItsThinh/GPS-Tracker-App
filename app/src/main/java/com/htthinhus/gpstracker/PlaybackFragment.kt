package com.htthinhus.gpstracker

import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.graphics.scale
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import com.htthinhus.gpstracker.activities.MainActivity
import com.htthinhus.gpstracker.databinding.FragmentPlaybackBinding
import com.htthinhus.gpstracker.viewmodels.MainActivityViewModel
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.annotation.AnnotationConfig
import com.mapbox.maps.plugin.annotation.AnnotationPlugin
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotation
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

class PlaybackFragment : Fragment() {

    private lateinit var binding: FragmentPlaybackBinding
    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()

    private val args: PlaybackFragmentArgs by navArgs()
    private var startTimeSeconds: Long? = null
    private var endTimeSeconds: Long? = null

    private val pointList = mutableListOf<Point>()
    private val timestampSecondList = mutableListOf<Long>()
    private val pointListPolyline = mutableListOf<Point>()
    private var currentIndexPolylinePoint = 0
    private var animator: ValueAnimator? = null

    private var currentIndex: Int = 0
    private var previousProgress: Int = 0

    private lateinit var myGPSAnnotation: PointAnnotation
    private lateinit var pointAnnotationManager: PointAnnotationManager
    private lateinit var annotationPlugin: AnnotationPlugin

    private val handler: Handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null

    private var polylineAnnotationManager: PolylineAnnotationManager? = null
    private val polylineAnnotations = mutableListOf<PolylineAnnotation>()

    private var isPlaybackRunning = true
    private var isDonePlayback = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPlaybackBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainActivityViewModel.hideBottomNav()

        startTimeSeconds = args.startTime
        endTimeSeconds = args.endTime

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        dateFormat.timeZone = TimeZone.getTimeZone("GMT+7")
        val startTimeDate = dateFormat.format(Date(startTimeSeconds!!*1000))
        val endTimeDate = dateFormat.format(Date(endTimeSeconds!!*1000))
        Log.d("PLAYBACK_FRAGMENT", startTimeDate)
        Log.d("PLAYBACK_FRAGMENT", endTimeDate)

        getFirestoreData()

        binding.btnPlaybackControl.setOnClickListener {
            if (!isDonePlayback) {
                togglePlaybackControl()
            } else {
                resetPlayback()
            }
        }
    }

    private fun getFirestoreData() {
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
                        timestampSecondList.add(timestampSecond)
                    }

                    pointListPolyline.add(pointList[0])
                    currentIndexPolylinePoint = 1

                    setupMap(pointList[0])
                    initializeMarker(pointList[0])
                    startPlayback()
                }
            }
    }

    private fun setupMap(startPoint: Point) {

        binding.mapView.mapboxMap.loadStyle(style(Style.OUTDOORS) {}) {
            annotationPlugin = binding.mapView.annotations
            polylineAnnotationManager = annotationPlugin.createPolylineAnnotationManager(
                annotationConfig = AnnotationConfig("pitch_outline", "line_source", "line_layer")
            )
        }

        val cameraOptions = CameraOptions.Builder()
            .center(startPoint)
            .zoom(15.0)
            .build()
        binding.mapView.mapboxMap.easeTo(
            cameraOptions,
            MapAnimationOptions.mapAnimationOptions {
                duration(0)
            }
        )
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

    private fun startPlayback() {
        val seekBar = binding.seekBarPlayback
        seekBar.max = pointList.size - 1
        seekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentIndex = progress
                updateMarker(pointList[currentIndex], 0)
                if (progress > previousProgress) {
                    addPoint()
                } else {
                    deleteLastPoint()
                }
                previousProgress = progress

                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                dateFormat.timeZone = TimeZone.getTimeZone("GMT+7")
                binding.tvDateTime.text = dateFormat.format(Date(timestampSecondList[currentIndex]*1000))
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                handler.removeCallbacks(runnable!!)
                isPlaybackRunning = false
                binding.btnPlaybackControl.setIconResource(R.drawable.baseline_play_arrow_24)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}

        })
        startUpdatingMarker()
    }

    private fun addPoint() {
        if(currentIndexPolylinePoint < pointList.size) {
            pointListPolyline.add(pointList[currentIndexPolylinePoint])
            currentIndexPolylinePoint++
            updatePolyline()
        } else {
            Toast.makeText(context, "no more point to add", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteLastPoint() {
        if(pointListPolyline.isNotEmpty()) {
            pointListPolyline.removeAt(pointListPolyline.size - 1)
            currentIndexPolylinePoint--
            updatePolyline()
        }
    }

    private fun updatePolyline() {
        if(pointListPolyline.size > 1){
            val polylineAnnotationOptions = PolylineAnnotationOptions()
                .withPoints(pointListPolyline)
                .withLineColor(Color.BLUE)
                .withLineWidth(3.0)

            val polylineAnnotation = polylineAnnotationManager?.create(polylineAnnotationOptions)
            polylineAnnotation?.let {
                polylineAnnotations.add(it)
            }
        }
        if(pointListPolyline.size <= 1) {
            polylineAnnotationManager!!.deleteAll()
        }
    }

    private fun startUpdatingMarker() {
        runnable = object: Runnable{
            override fun run() {
                if (currentIndex >= pointList.size) {
                    handler.removeCallbacks(runnable!!)
                    binding.btnPlaybackControl.setIconResource(R.drawable.baseline_play_arrow_24)
                    isPlaybackRunning = false
                    isDonePlayback = true
                    return
                }
                updateMarker(pointList[currentIndex], 0)
                binding.seekBarPlayback.progress = currentIndex
                handler.postDelayed(this, 1000)
                currentIndex++
            }
        }
        handler.post(runnable!!)
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

    private fun togglePlaybackControl() {
        if (isPlaybackRunning) {
            handler.removeCallbacks(runnable!!)
            binding.btnPlaybackControl.setIconResource(R.drawable.baseline_play_arrow_24)
        } else {
            handler.post(runnable!!)
            binding.btnPlaybackControl.setIconResource(R.drawable.baseline_pause_24)
        }
        isPlaybackRunning = !isPlaybackRunning
    }

    private fun resetPlayback() {
        isDonePlayback = false
        binding.seekBarPlayback.progress = 0
        currentIndex = 0
        currentIndexPolylinePoint = 1
        pointListPolyline.clear()
        pointListPolyline.add(pointList[0])
        polylineAnnotationManager!!.deleteAll()
        binding.btnPlaybackControl.setIconResource(R.drawable.baseline_pause_24)
        isPlaybackRunning = !isPlaybackRunning
        handler.post(runnable!!)
    }

}