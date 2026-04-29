package com.financeobserver.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.financeobserver.R
import com.financeobserver.util.LocaleHelper
import com.google.android.material.button.MaterialButton

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private lateinit var grantNotificationBtn: MaterialButton
    private lateinit var grantSmsBtn: MaterialButton
    private lateinit var englishBtn: MaterialButton
    private lateinit var arabicBtn: MaterialButton
    private lateinit var permStatusText: TextView
    private lateinit var permDot: View

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        grantNotificationBtn = view.findViewById(R.id.grantNotificationBtn)
        grantSmsBtn = view.findViewById(R.id.grantSmsBtn)
        englishBtn = view.findViewById(R.id.englishBtn)
        arabicBtn = view.findViewById(R.id.arabicBtn)
        permStatusText = view.findViewById(R.id.permStatusText)
        permDot = view.findViewById(R.id.permDot)

        setupPermissionButtons()
        setupLanguageButtons()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    private fun setupPermissionButtons() {
        grantNotificationBtn.setOnClickListener {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
        }

        grantSmsBtn.setOnClickListener {
            requestPermissions(
                arrayOf(android.Manifest.permission.READ_SMS, android.Manifest.permission.RECEIVE_SMS),
                SMS_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun setupLanguageButtons() {
        val currentLocale = LocaleHelper.getCurrentLocale(requireContext())

        englishBtn.setOnClickListener {
            if (currentLocale != LocaleHelper.LOCALE_EN) {
                showLanguageChangeDialog(LocaleHelper.LOCALE_EN)
            }
        }

        arabicBtn.setOnClickListener {
            if (currentLocale != LocaleHelper.LOCALE_AR) {
                showLanguageChangeDialog(LocaleHelper.LOCALE_AR)
            }
        }

        updateLanguageButtonStates(currentLocale)
    }

    private fun updateLanguageButtonStates(locale: String) {
        val isEnglish = locale == LocaleHelper.LOCALE_EN
        englishBtn.strokeWidth = if (isEnglish) 3 else 0
        englishBtn.text = "English ${if (isEnglish) "\u2713" else ""}"
        arabicBtn.strokeWidth = if (!isEnglish) 3 else 0
        arabicBtn.text = "\u0627\u0644\u0639\u0631\u0628\u064A\u0629 ${if (!isEnglish) "\u2713" else ""}"
    }

    private fun showLanguageChangeDialog(languageCode: String) {
        val languageName = if (languageCode == LocaleHelper.LOCALE_AR) "\u0627\u0644\u0639\u0631\u0628\u064A\u0629" else "English"
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.change_language))
            .setMessage(getString(R.string.change_language_msg, languageName))
            .setPositiveButton(getString(R.string.restart)) { _, _ ->
                LocaleHelper.saveLocale(requireContext(), languageCode)
                requireActivity().recreate()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun updatePermissionStatus() {
        val hasNotificationAccess = checkNotificationAccess()
        val hasSmsPermission = requireContext().checkSelfPermission(
            android.Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasNotificationAccess && hasSmsPermission) {
            permStatusText.text = getString(R.string.active_status)
            permStatusText.setTextColor(requireContext().getColor(R.color.accent_positive))
            permDot.setBackgroundColor(requireContext().getColor(R.color.accent_positive))
        } else {
            permStatusText.text = getString(R.string.setup_needed_status)
            permStatusText.setTextColor(requireContext().getColor(R.color.accent_critical))
            permDot.setBackgroundColor(requireContext().getColor(R.color.accent_critical))
        }
    }

    private fun checkNotificationAccess(): Boolean {
        val enabledPackages = Settings.Secure.getString(
            requireContext().contentResolver,
            "enabled_notification_listeners"
        ) ?: ""
        return enabledPackages.contains(requireContext().packageName)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            updatePermissionStatus()
            (activity as? MainActivity)?.checkPermissionsAndLoad()
        }
    }

    companion object {
        private const val SMS_PERMISSION_REQUEST_CODE = 2001
    }
}
