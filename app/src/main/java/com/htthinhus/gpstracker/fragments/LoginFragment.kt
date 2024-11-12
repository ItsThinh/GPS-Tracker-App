package com.htthinhus.gpstracker.fragments

import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.htthinhus.gpstracker.R
import com.htthinhus.gpstracker.databinding.FragmentLoginBinding
import com.htthinhus.gpstracker.utils.MySharedPreferences
import com.htthinhus.gpstracker.viewmodels.UserViewModel

class LoginFragment : Fragment() {

    private var backPressedOnce = false
    private val backPressHandler = android.os.Handler(Looper.getMainLooper())
    private val backPressRunnable = Runnable {
        backPressedOnce = false
    }

    private lateinit var mySharedPreferences: MySharedPreferences
    private val userViewModel: UserViewModel by activityViewModels()
    private lateinit var savedStateHandle: SavedStateHandle

    private val auth = Firebase.auth

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        savedStateHandle = findNavController().previousBackStackEntry!!.savedStateHandle
        savedStateHandle[LOGIN_SUCCESSFUL] = false

        mySharedPreferences = MySharedPreferences(requireContext())

        binding.btnLogIn.setOnClickListener {
            login()
        }

        binding.tvToSignup.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signupFragment)
        }

        setupBackPressBehavior()
    }

    fun login() {
        val auth = Firebase.auth

        val emailEditText = binding.etEmail
        val passwordEditText = binding.etPassword
        val email = emailEditText.text.toString()
        val password = passwordEditText.text.toString()

        userViewModel.login(email, password).observe(viewLifecycleOwner, Observer { result ->
            if (result) {
                savedStateHandle[LOGIN_SUCCESSFUL] = true

//                getFuel()

                sendTokenToFirestore()

                // get GPS Device Id
                FirebaseFirestore.getInstance()
                    .collection("devices")
                    .whereEqualTo("userId", auth.currentUser!!.uid)
                    .get()
                    .addOnSuccessListener { documents ->
                        if(!documents.isEmpty){
                            val document = documents.documents[0]
                            if (document.exists()) {
                                val documentId = document.id
                                mySharedPreferences.setDeviceId(documentId)
                            }
                        }
                        findNavController().popBackStack()
                    }

            } else {
                Toast.makeText(context, "Login failed", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupBackPressBehavior() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (backPressedOnce) {
                    requireActivity().finishAffinity()
                } else {
                    backPressedOnce = true
                    Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                    backPressHandler.postDelayed(backPressRunnable, 2000)
                }
            }
        })
    }

//    private fun getFuel() {
//
//        if (auth.currentUser != null) {
//            FirebaseFirestore.getInstance()
//                .collection("users")
//                .document(auth.currentUser!!.uid)
//                .get()
//                .addOnSuccessListener { document ->
//                    if (document.exists()) {
//                        val fuelValue = document.getLong("fuelConsumption100km")
//                        mySharedPreferences.setFuelConsumption100km(fuelValue?.toFloat() ?: 0)
//                    }
//                }
//                .addOnFailureListener {
//                    Log.d("LOGIN_FRAGMENT", "Can't get fuel value: $it")
//                }
//        }
//    }

    private fun sendTokenToFirestore() {

        val token = mySharedPreferences.getToken()

        if (auth.currentUser != null && token != null) {
            val userId = auth.currentUser!!.uid

            val firestoreRef = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("fcmTokens")
                .document(token)

            firestoreRef.set(emptyMap<String, Any>())
                .addOnSuccessListener {
                    Log.d("SENDING_FCM_TOKEN", "Token sent")
                }
                .addOnFailureListener {
                    Log.d("SENDING_FCM_TOKEN", it.toString())
                }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val LOGIN_SUCCESSFUL = "LOGIN_SUCCESSFUL"
    }

}