package com.htthinhus.gpstracker.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.htthinhus.gpstracker.R
import com.htthinhus.gpstracker.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

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
        auth = Firebase.auth

        val currentUser = auth.currentUser
        if(currentUser!=null) {
            Toast.makeText(requireContext(), "Already login", Toast.LENGTH_SHORT).show()
        } else Toast.makeText(requireContext(), "Not login", Toast.LENGTH_SHORT).show()

        binding.btnLogIn.setOnClickListener {
            if(true) {
                auth.signInWithEmailAndPassword(binding.etEmail.text.toString(), binding.etPassword.text.toString())
                    .addOnCompleteListener() {  task ->
                        if(task.isSuccessful) {
                            val user = auth.currentUser
                        } else {
                            Log.w("LOGIN_FRAGMENT", "SignIn with Email: failure", task.exception)

                        }
                    }
            }
        }

        binding.tvToSignup.setOnClickListener {
            val navController = findNavController().navigate(R.id.action_loginFragment_to_signupFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}