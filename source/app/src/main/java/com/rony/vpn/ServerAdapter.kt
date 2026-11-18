package com.rony.vpn

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ServerAdapter(
    private var servers: List<Server>,
    private var selectedId: String?,
    private val onSelect: (Server) -> Unit
) : RecyclerView.Adapter<ServerAdapter.Holder>() {

    class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val flag: TextView = v.findViewById(R.id.flag)
        val name: TextView = v.findViewById(R.id.name)
        val detail: TextView = v.findViewById(R.id.detail)
        val mark: TextView = v.findViewById(R.id.mark)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_server, parent, false)
        return Holder(v)
    }

    override fun onBindViewHolder(h: Holder, position: Int) {
        val s = servers[position]
        h.flag.text = s.flag
        h.name.text = s.country
        val ping = if (s.pingMs >= 0) "${s.pingMs} ms" else "pinging…"
        h.detail.text = "${s.host} • ${s.ip} • ${s.proto.uppercase()} • $ping"
        val selected = s.id == selectedId
        h.mark.visibility = if (selected) View.VISIBLE else View.INVISIBLE
        h.itemView.isSelected = selected
        h.itemView.setOnClickListener { onSelect(s) }
    }

    override fun getItemCount(): Int = servers.size

    fun update(newServers: List<Server>, newSelectedId: String?) {
        servers = newServers
        selectedId = newSelectedId
        notifyDataSetChanged()
    }
}
