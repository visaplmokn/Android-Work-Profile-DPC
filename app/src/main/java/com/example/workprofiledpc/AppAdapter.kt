package com.example.workprofiledpc

import android.view.LayoutInflater
import android.view.ViewGroup
import android.content.pm.ApplicationInfo
import androidx.recyclerview.widget.RecyclerView
import com.example.workprofiledpc.databinding.ItemAppBinding

class AppAdapter(
    private val apps: List<ApplicationInfo>,
    private val onCloneClick: (ApplicationInfo) -> Unit
) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    inner class AppViewHolder(val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        val pm = holder.itemView.context.packageManager
        
        holder.binding.txtAppName.text = app.loadLabel(pm).toString()
        holder.binding.txtPackageName.text = app.packageName
        holder.binding.imgAppIcon.setImageDrawable(app.loadIcon(pm))

        holder.binding.btnClone.setOnClickListener {
            onCloneClick(app)
        }
    }

    override fun getItemCount(): Int = apps.size
}
