package com.example.dpc

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private lateinit var dpm: DevicePolicyManager
    private lateinit var adminComponent: ComponentName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, MyDeviceAdminReceiver::class.java)

        val btnCreate: Button = findViewById(R.id.btnCreateProfile)
        val btnDelete: Button = findViewById(R.id.btnDeleteProfile)
        val rvApps: RecyclerView = findViewById(R.id.rvInstalledApps)

        btnCreate.setOnClickListener {
            val intent = Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE).apply {
                putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME, adminComponent)
            }
            startActivityForResult(intent, 100)
        }

        btnDelete.setOnClickListener {
            try {
                dpm.wipeData(0)
                Toast.makeText(this, "تم حذف ملف تعريف العمل بنجاح", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "خطأ أثناء الحذف: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // إعداد قائمة التطبيقات المثبتة في الملف الشخصي لنسخها
        val pm = packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 } // تطبيقات المستخدم فقط

        rvApps.layoutManager = LinearLayoutManager(this)
        rvApps.adapter = AppAdapter(installedApps, pm) { appInfo ->
            cloneAppToWorkProfile(appInfo.packageName)
        }
    }

    private fun cloneAppToWorkProfile(packageName: String) {
        try {
            if (dpm.isProfileOwnerApp(packageName)) {
                Toast.makeText(this, "هذا التطبيق هو مالك الملف بالفعل", Toast.LENGTH_SHORT).show()
                return
            }
            dpm.setApplicationHidden(adminComponent, packageName, false)
            Toast.makeText(this, "تم تفعيل التطبيق داخل ملف العمل", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "حدث خطأ: تأكد من تفعيل صلاحيات الـ Admin أولاً", Toast.LENGTH_LONG).show()
        }
    }
}

// الـ Adapter لعرض قائمة التطبيقات
class AppAdapter(
    private val apps: List<ApplicationInfo>,
    private val pm: PackageManager,
    private val onCloneClick: (ApplicationInfo) -> Unit
) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appName: TextView = view.findViewById(R.id.tvAppName)
        val btnClone: Button = view.findViewById(R.id.btnClone)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        holder.appName.text = app.loadLabel(pm).toString()
        holder.btnClone.setOnClickListener { onCloneClick(app) }
    }

    override fun getItemCount(): Int = apps.size
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
