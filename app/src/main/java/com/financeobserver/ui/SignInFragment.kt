package com.financeobserver.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.financeobserver.FinanceObserverApp
import com.financeobserver.R
import com.financeobserver.service.AuthManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SignInFragment : Fragment() {

    private lateinit var app: FinanceObserverApp
    private lateinit var authManager: AuthManager
    private val fragmentScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sign_in, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        app = requireActivity().application as FinanceObserverApp
        authManager = app.authManager

        val phoneInput = view.findViewById<TextInputEditText>(R.id.phoneInput)
        val passwordInput = view.findViewById<TextInputEditText>(R.id.passwordInput)
        val signInBtn = view.findViewById<MaterialButton>(R.id.signInBtn)
        val biometricBtn = view.findViewById<MaterialButton>(R.id.biometricBtn)
        val signUpLink = view.findViewById<TextView>(R.id.signUpLink)

        signInBtn.setOnClickListener {
            val phone = phoneInput.text?.toString()?.trim() ?: ""
            val password = passwordInput.text?.toString() ?: ""
            if (phone.isBlank()) {
                phoneInput.error = getString(R.string.auth_required)
                return@setOnClickListener
            }
            if (password.isBlank()) {
                passwordInput.error = getString(R.string.auth_required)
                return@setOnClickListener
            }
            signIn(phone, password)
        }

        signUpLink.setOnClickListener {
            (activity as? MainActivity)?.showSignUp()
        }

        fragmentScope.launch {
            if (authManager.isBiometricAvailable() && authManager.isBiometricEnabled()) {
                biometricBtn.visibility = View.VISIBLE
                biometricBtn.setOnClickListener { showBiometricPrompt() }
            }
        }
    }

    private fun signIn(phone: String, password: String) {
        fragmentScope.launch {
            val result = authManager.signIn(phone, password)
            result.onSuccess {
                (activity as? MainActivity)?.onAuthSuccess()
            }.onFailure { e ->
                Toast.makeText(requireContext(), e.message ?: "Sign in failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(requireContext())
        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                Toast.makeText(requireContext(), R.string.auth_biometric_success, Toast.LENGTH_SHORT).show()
                (activity as? MainActivity)?.onAuthSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Toast.makeText(requireContext(), errString, Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationFailed() {
                Toast.makeText(requireContext(), R.string.auth_biometric_failed, Toast.LENGTH_SHORT).show()
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.auth_biometric_title))
            .setSubtitle(getString(R.string.auth_biometric_subtitle))
            .setNegativeButtonText(getString(R.string.cancel))
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
