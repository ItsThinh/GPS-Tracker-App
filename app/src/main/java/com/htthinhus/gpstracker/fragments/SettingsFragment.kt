package com.htthinhus.gpstracker.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.htthinhus.gpstracker.R
import com.htthinhus.gpstracker.databinding.FragmentSettingsBinding
import com.htthinhus.gpstracker.utils.MySharedPreferences
import com.htthinhus.gpstracker.viewmodels.UserViewModel

class SettingsFragment : PreferenceFragmentCompat() {

    private val userViewModel: UserViewModel by activityViewModels()

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private var DEVICE_ID: String? = null

    private lateinit var mySharedPreference: MySharedPreferences

    private lateinit var fuelCurrentLevel: EditTextPreference
    private lateinit var fuelConsumption: EditTextPreference
    private lateinit var tankCapacity: EditTextPreference
    private lateinit var fuelWarningPercentage: EditTextPreference

    private val auth = Firebase.auth


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val preferenceScreenView = super.onCreateView(inflater, binding.settingsContainer, savedInstanceState)
        binding.settingsContainer.addView(preferenceScreenView)
        DEVICE_ID = mySharedPreference.getDeviceId()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navController = findNavController()

        userViewModel.loginState.observe(viewLifecycleOwner, Observer { loginState ->
            if(!loginState) {
                findNavController().navigate(R.id.loginFragment)
            } else {
                binding.tvUserEmail.text = auth.currentUser!!.email
                if (mySharedPreference.getDeviceId() != null) {
                    binding.tvDeviceId.text = mySharedPreference.getDeviceId()
                }
            }
        })

        findPreference<EditTextPreference>("fuelCurrentLevel")?.summary =
            "Current value: ${mySharedPreference.getCurrentFuelLevel()} L"

        findPreference<EditTextPreference>("fuelConsumption")?.summary =
            "Current value: ${mySharedPreference.getFuelConsumption100km()} L"

        findPreference<EditTextPreference>("tankCapacity")?.summary =
            "Current value: ${mySharedPreference.getTankCapacity()} L"

        findPreference<EditTextPreference>("fuelWarningPercentage")?.summary =
            "Current value: ${mySharedPreference.getWarningFuelPercentage()} %"

        binding.btnSignout.setOnClickListener {
            userViewModel.logout()
            mySharedPreference.setDeviceId(null)
        }

        setUpSettings()

        // navigate to map fragment if from login fragment navigate to this fragment
        val currentBackStackEntry = navController.currentBackStackEntry!!
        val savedStateHandle = currentBackStackEntry.savedStateHandle
        savedStateHandle.getLiveData<Boolean>(LoginFragment.LOGIN_SUCCESSFUL)
            .observe(viewLifecycleOwner, Observer { success ->
                if (success != null && success) {
                    val startDestination = navController.graph.startDestinationId
                    val navOptions = NavOptions.Builder()
                        .setPopUpTo(startDestination, true)
                        .build()
                    navController.navigate(startDestination, null, navOptions)
                }
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setUpSettings() {

        findPreference<EditTextPreference>("fuelCurrentLevel")
            ?.setOnPreferenceChangeListener { preference, newValue ->

                val newValueString = newValue.toString().trim()
                val newFuelValueFloat = newValueString.toFloatOrNull()

                if (newFuelValueFloat != null) {
                    fuelCurrentLevel.summary = "Value: $newValueString L"
                    addFuelToDatabase(newFuelValueFloat, "currentFuelLevel")
                } else {
                    Toast.makeText(context, "Please enter a valid float number", Toast.LENGTH_SHORT).show()
                }
                true
            }

        findPreference<EditTextPreference>("fuelConsumption")
            ?.setOnPreferenceChangeListener { preference, newValue ->

                val newValueString = newValue.toString().trim()
                val newFuelValueFloat = newValueString.toFloatOrNull()

                if (newFuelValueFloat != null) {
                    fuelConsumption.summary = "Value: $newValueString L"
                    addFuelToDatabase(newFuelValueFloat, "fuelConsumptionPer100km")
                } else {
                    Toast.makeText(context, "Please enter a valid float number", Toast.LENGTH_SHORT).show()
                }
                true
            }

        findPreference<EditTextPreference>("tankCapacity")
            ?.setOnPreferenceChangeListener { preference, newValue ->

                val newValueString = newValue.toString().trim()
                val newFuelValueFloat = newValueString.toFloatOrNull()

                if (newFuelValueFloat != null) {
                    tankCapacity.summary = "Value: $newValueString L"
                    addFuelToDatabase(newFuelValueFloat, "tankCapacity")
                } else {
                    Toast.makeText(context, "Please enter a valid float number", Toast.LENGTH_SHORT).show()
                }
                true
            }

        findPreference<EditTextPreference>("fuelWarningPercentage")
            ?.setOnPreferenceChangeListener { preference, newValue ->

                val newValueString = newValue.toString().trim()
                val newFuelValueFloat = newValueString.toFloatOrNull()

                if (newFuelValueFloat != null) {
                    fuelWarningPercentage.summary = "Value: $newValueString L"
                    addFuelToDatabase(newFuelValueFloat, "warningFuelPercentage")
                } else {
                    Toast.makeText(context, "Please enter a valid float number", Toast.LENGTH_SHORT).show()
                }
                true
            }

        findPreference<EditTextPreference>("deviceId")
            ?.setOnPreferenceChangeListener { preference, newValue ->
                val newDeviceIdString = newValue.toString().trim()
                FirebaseFirestore.getInstance().collection("devices").document(newDeviceIdString)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            mySharedPreference.setDeviceId(newDeviceIdString)

                            val userId = hashMapOf("userId" to auth.currentUser!!.uid)
                            FirebaseFirestore.getInstance().collection("devices").document(newDeviceIdString)
                                .set(userId)
                                .addOnSuccessListener { Toast.makeText(context, "Device ID is added", Toast.LENGTH_SHORT).show() }
                                .addOnFailureListener { Toast.makeText(context, "Failed to add device ID, try again later", Toast.LENGTH_SHORT).show() }
                        } else {
                            Toast.makeText(context, "Device ID is not exist", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to connect, try again later", Toast.LENGTH_SHORT).show()
                    }
                true
            }
    }

    private fun addFuelToDatabase(fuelValue: Float, childString: String) {
        FirebaseDatabase.getInstance().getReference(DEVICE_ID!!).child("fuelData").child(childString)
            .setValue(fuelValue)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        mySharedPreference = MySharedPreferences(requireContext())

        fuelCurrentLevel = findPreference("fuelCurrentLevel")!!
        fuelConsumption = findPreference("fuelConsumption")!!
        tankCapacity = findPreference("tankCapacity")!!
        fuelWarningPercentage = findPreference("fuelWarningPercentage")!!

        fuelCurrentLevel.summary = mySharedPreference.getCurrentFuelLevel().toString() + " L"
        fuelConsumption.summary = mySharedPreference.getFuelConsumption100km().toString() + " L"
        tankCapacity.summary = mySharedPreference.getTankCapacity().toString() + " L"
        fuelWarningPercentage.summary = mySharedPreference.getWarningFuelPercentage().toString() + " %"
    }
}