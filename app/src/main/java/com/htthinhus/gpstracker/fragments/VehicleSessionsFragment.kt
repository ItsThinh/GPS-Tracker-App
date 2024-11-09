package com.htthinhus.gpstracker.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.htthinhus.gpstracker.R
import com.htthinhus.gpstracker.adapters.VehicleSessionAdapter
import com.htthinhus.gpstracker.databinding.FragmentVehicleSessionsBinding
import com.htthinhus.gpstracker.models.VehicleSession
import com.htthinhus.gpstracker.viewmodels.MainActivityViewModel

class VehicleSessionsFragment : Fragment() {
    private var _binding: FragmentVehicleSessionsBinding? = null
    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()
    private val binding get() = _binding!!

    private lateinit var vehicleSessionList: ArrayList<VehicleSession>

    private lateinit var vehicleSessionAdapter: VehicleSessionAdapter
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

        mainActivityViewModel.showBottomNav()

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
                vehicleSessionAdapter = VehicleSessionAdapter(vehicleSessionList)
                binding.rvVehicleSessions.adapter = vehicleSessionAdapter

                vehicleSessionAdapter.onItemClick = { vehicleSession ->
                    val direction =
                        VehicleSessionsFragmentDirections
                            .actionVehicleSessionsFragmentToVehicleSessionDetailFragment(
                                vehicleSession.startTime!!.seconds,
                                vehicleSession.endTime!!.seconds
                            )
                    findNavController().navigate(direction)
                }
            }
            .addOnFailureListener { exception ->
                Log.w("VEHICLE_SESSIONS", exception)
            }

        binding.btnSelectTime.setOnClickListener {
            findNavController().navigate(R.id.action_vehicleSessionsFragment_to_dateRangePickerFragment)
        }
    }
}