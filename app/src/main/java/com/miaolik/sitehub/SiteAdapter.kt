package com.miaolik.sitehub

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SiteAdapter(
    private var sites: List<Site>,
    private val onOpen: (Site) -> Unit,
    private val onEdit: (Site) -> Unit,
) : RecyclerView.Adapter<SiteAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.name)
        val url: TextView = view.findViewById(R.id.url)
        val defaultBadge: TextView = view.findViewById(R.id.defaultBadge)
    }

    fun submit(items: List<Site>) {
        sites = items
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_site, parent, false),
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val site = sites[position]
        holder.name.text = site.name
        holder.url.text = site.url()
        holder.defaultBadge.visibility = if (site.isDefault) View.VISIBLE else View.GONE
        holder.itemView.setOnClickListener { onOpen(site) }
        holder.itemView.setOnLongClickListener {
            onEdit(site)
            true
        }
    }

    override fun getItemCount() = sites.size
}
