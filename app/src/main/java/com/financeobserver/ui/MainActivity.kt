package com.financeobserver.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.financeobserver.FinanceObserverApp
import com.financeobserver.R
import com.financeobserver.model.Transaction
import com.financeobserver.util.CurrencyHelper
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var app: FinanceObserverApp
    private lateinit var bottomNav: BottomNavigationView

    private val homeFragment = HomeFragment()
    private val transactionsFragment = TransactionsFragment()
    private val subscriptionsFragment = SubscriptionsFragment()
    private val accountsFragment = AccountsFragment()
    private val settingsFragment = SettingsFragment()

    private var activeFragment: Fragment = homeFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        app = application as FinanceObserverApp
        bottomNav = findViewById(R.id.bottomNav)

        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, settingsFragment, "settings").hide(settingsFragment)
            .add(R.id.fragmentContainer, accountsFragment, "accounts").hide(accountsFragment)
            .add(R.id.fragmentContainer, subscriptionsFragment, "subscriptions").hide(subscriptionsFragment)
            .add(R.id.fragmentContainer, transactionsFragment, "transactions").hide(transactionsFragment)
            .add(R.id.fragmentContainer, homeFragment, "home")
            .commit()

        setupBottomNav()
    }

    private fun setupBottomNav() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> switchFragment(homeFragment)
                R.id.nav_transactions -> switchFragment(transactionsFragment)
                R.id.nav_subscriptions -> switchFragment(subscriptionsFragment)
                R.id.nav_accounts -> switchFragment(accountsFragment)
                R.id.nav_settings -> switchFragment(settingsFragment)
                else -> false
            }
        }
    }

    private fun switchFragment(target: Fragment): Boolean {
        if (target == activeFragment) return true
        supportFragmentManager.beginTransaction()
            .hide(activeFragment)
            .show(target)
            .commit()
        activeFragment = target
        return true
    }

    override fun onResume() {
        super.onResume()
        checkPermissionsAndLoad()
    }

    private fun checkNotificationAccess(): Boolean {
        val enabledPackages = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        ) ?: ""
        return enabledPackages.contains(packageName)
    }

    fun checkPermissionsAndLoad() {
        val hasNotificationAccess = checkNotificationAccess()
        val hasSmsPermission = checkSelfPermission(
            android.Manifest.permission.READ_SMS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasNotificationAccess && hasSmsPermission) {
            homeFragment.refresh()
            transactionsFragment.refresh()
            subscriptionsFragment.refresh()
            accountsFragment.refresh()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            checkPermissionsAndLoad()
        }
    }

    companion object {
        const val SMS_PERMISSION_REQUEST_CODE = 1001
    }
}
