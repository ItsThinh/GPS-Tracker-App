package com.htthinhus.gpstracker.fragments

import android.os.Bundle
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.htthinhus.gpstracker.R
import com.htthinhus.gpstracker.databinding.FragmentSignupBinding

class SignupFragment : Fragment() {

    private var backPressedOnce = false
    private val backPressHandler = android.os.Handler(Looper.getMainLooper())
    private val backPressRunnable = Runnable {
        backPressedOnce = false
    }

    private lateinit var auth: FirebaseAuth
    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth

        binding.btnSignUp.setOnClickListener {
            auth.createUserWithEmailAndPassword(binding.etEmail.text.toString(), binding.etPassword.text.toString())
                .addOnCompleteListener { task ->
                    if(task.isSuccessful) {
                        Toast.makeText(context, "Create account successfully", Toast.LENGTH_SHORT).show()
                        val user = auth.currentUser
                    }
                    else {
                        Toast.makeText(context, "Create account failed", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        binding.tvToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_signupFragment_to_loginFragment)
        }

        setupBackPressBehavior()
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


}