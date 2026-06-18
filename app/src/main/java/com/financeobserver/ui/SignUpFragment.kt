package com.financeobserver.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.financeobserver.FinanceObserverApp
import com.financeobserver.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SignUpFragment : Fragment() {

    private lateinit var app: FinanceObserverApp
    private val fragmentScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sign_up, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        app = requireActivity().application as FinanceObserverApp

        val phoneInput = view.findViewById<TextInputEditText>(R.id.phoneInput)
        val passwordInput = view.findViewById<TextInputEditText>(R.id.passwordInput)
        val confirmPasswordInput = view.findViewById<TextInputEditText>(R.id.confirmPasswordInput)
        val signUpBtn = view.findViewById<MaterialButton>(R.id.signUpBtn)
        val signInLink = view.findViewById<TextView>(R.id.signInLink)

        signUpBtn.setOnClickListener {
            val phone = phoneInput.text?.toString()?.trim() ?: ""
            val password = passwordInput.text?.toString() ?: ""
            val confirmPassword = confirmPasswordInput.text?.toString() ?: ""

            if (phone.isBlank()) {
                phoneInput.error = getString(R.string.auth_required)
                return@setOnClickListener
            }
            if (password.length < 4) {
                passwordInput.error = getString(R.string.auth_password_short)
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                confirmPasswordInput.error = getString(R.string.auth_passwords_mismatch)
                return@setOnClickListener
            }
            signUp(phone, password)
        }

        signInLink.setOnClickListener {
            (activity as? MainActivity)?.showSignIn()
        }
    }

    private fun signUp(phone: String, password: String) {
        fragmentScope.launch {
            val result = app.authManager.signUp(phone, password)
            result.onSuccess {
                Toast.makeText(requireContext(), R.string.auth_sign_up_success, Toast.LENGTH_SHORT).show()
                (activity as? MainActivity)?.onAuthSuccess()
            }.onFailure { e ->
                Toast.makeText(requireContext(), e.message ?: "Sign up failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
