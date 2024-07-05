package com.htthinhus.gpstracker.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.htthinhus.gpstracker.R
import com.htthinhus.gpstracker.databinding.FragmentSettingsBinding
import com.htthinhus.gpstracker.utils.MySharedPreferences

class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var auth: FirebaseAuth

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var mySharedPreference: MySharedPreferences

    private lateinit var fuelPreference: EditTextPreference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val preferenceScreenView = super.onCreateView(inflater, binding.settingsContainer, savedInstanceState)
        binding.settingsContainer.addView(preferenceScreenView)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth
        binding.btnSignout.setOnClickListener {
            auth.signOut()
        }

        setUpSettings()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setUpSettings() {
        findPreference<SwitchPreferenceCompat>("notifications")
            ?.setOnPreferenceChangeListener { preference, newValue ->
                Toast.makeText(context, "Notifications enabled: ${newValue}", Toast.LENGTH_SHORT).show()
                true
            }

        findPreference<Preference>("feedback")
            ?.setOnPreferenceClickListener {

                val preference =
                    PreferenceManager.getDefaultSharedPreferences(requireContext()).all
                preference.forEach {
                    Log.d("SETTINGS_PREFERENCE", "${it.key} -> ${it.value}")
                }

                true
            }

        findPreference<EditTextPreference>("fuel")
            ?.setOnPreferenceChangeListener { preference, newValue ->
                val newValueString = newValue.toString().trim()
                val newValueInt = newValueString.toIntOrNull()
                if (newValueInt != null) {
                    mySharedPreference.setFuelConsumption100km(newValue.toString().trim().toInt())
                    fuelPreference.summary = mySharedPreference.getFuelConsumption100km().toString() + " L"
                    fuelPreference.text = ""
                } else {
                    Toast.makeText(context, "Please enter a valid integer number", Toast.LENGTH_SHORT).show()
                }

                true
            }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        mySharedPreference = MySharedPreferences(requireContext())

        fuelPreference = findPreference("fuel")!!
        fuelPreference.summary = mySharedPreference.getFuelConsumption100km().toString() + " L"
    }



}