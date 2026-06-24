package com.example.workprofiledpc

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.workprofiledpc.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dpm: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    private lateinit var appAdapter: AppAdapter
    private var isProfileOwner = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, MyDeviceAdminReceiver::class.java)
        
        isProfileOwner = dpm.isProfileOwnerApp(packageName)

        updateUiState()
        setupRecyclerView()

        binding.btnCreateProfile.setOnClickListener {
            if (isProfileOwner) {
                Toast.makeText(this, "Already a Work Profile Owner!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            provisionWorkProfile()
        }

        binding.btnDeleteProfile.setOnClickListener {
            if (!isProfileOwner) {
                Toast.makeText(this, "Must run inside Work Profile!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            wipeWorkProfile()
        }
    }

    private fun updateUiState() {
        if (isProfileOwner) {
            binding.txtStatus.text = "Status: WORK PROFILE ACTIVE (Profile Owner Mode)"
            binding.txtStatus.setTextColor(getColor(android.R.color.holo_green_dark))
            binding.btnCreateProfile.isEnabled = false
            binding.btnDeleteProfile.isEnabled = true
        } else {
            binding.txtStatus.text = "Status: PERSONAL PROFILE (DPC Inactive)"
            binding.txtStatus.setTextColor(getColor(android.R.color.holo_orange_dark))
            binding.btnCreateProfile.isEnabled = true
            binding.btnDeleteProfile.isEnabled = false
        }
    }

    private fun provisionWorkProfile() {
        val intent = Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE).apply {
            putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME, adminComponent)
            putExtra(DevicePolicyManager.EXTRA_PROVISIONING_SKIP_ENCRYPTION, true)
        }

        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, REQUEST_PROVISION_PROFILE)
        } else {
            Toast.makeText(this, "Work Profile Provisioning is unsupported on this device.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun wipeWorkProfile() {
        try {
            dpm.wipeData(0)
            Toast.makeText(this, "Deleting Work Profile...", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(this, "Security Exception: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupRecyclerView() {
        val installedApps = getInstalledAppsList()
        appAdapter = AppAdapter(installedApps) { appInfo ->
            cloneAppToWorkProfile(appInfo.packageName)
        }
        binding.recyclerViewApps.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewApps.adapter = appAdapter
    }

    private fun getInstalledAppsList(): List<ApplicationInfo> {
        val pm = packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA).filter { app ->
            (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0 && app.packageName != packageName
        }
    }

    private fun cloneAppToWorkProfile(targetPackage: String) {
        if (!isProfileOwner) {
            Toast.makeText(this, "Requires Profile Owner rights! Set using ADB.", Toast.LENGTH_LONG).show()
            return
        }

        try {
            dpm.setApplicationHidden(adminComponent, targetPackage, false)
            Toast.makeText(this, "Application unhidden in Work Profile container", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(this, "Cloning failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private const val REQUEST_PROVISION_PROFILE = 1001
    }
}
