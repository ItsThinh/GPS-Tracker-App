package com.htthinhus.gpstracker

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.htthinhus.gpstracker.databinding.FragmentVehicleSessionsBinding

class VehicleSessionsFragment : Fragment() {
    private var _binding: FragmentVehicleSessionsBinding? = null
    private val binding get() = _binding!!

    private lateinit var vehicleSessionList: ArrayList<VehicleSession>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentVehicleSessionsBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvVehicleSessions.layoutManager = LinearLayoutManager(context)

        vehicleSessionList = arrayListOf()

        val firestoreRef = FirebaseFirestore.getInstance()
            .collection("devices")
            .document("cf509abf-e231-43e0-a117-8b22bd25c7ed")
            .collection("sessions")
            .orderBy("startTime", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    for (document in documents) {
                        val session = document.toObject(VehicleSession::class.java)
                        vehicleSessionList.add(session)
                    }
                }
                binding.rvVehicleSessions.adapter = VehicleSessionAdapter(vehicleSessionList)
            }
            .addOnFailureListener { exception ->
                Log.w("VEHICLE_SESSIONS", exception)
            }

    }

}