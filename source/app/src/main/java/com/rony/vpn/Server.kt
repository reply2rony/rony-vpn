package com.rony.vpn

import org.json.JSONArray

/**
 * A VPN server embedded in the app. Configs come from the live VPN Gate
 * public list (see SERVER-LIST.md) — nothing here is invented.
 */
data class Server(
    val id: String,          // e.g. "jp-1"
    val country: String,     // e.g. "Japan"
    val short: String,       // e.g. "JP"
    val flag: String,        // e.g. "🇯🇵"
    val file: String,        // asset file, e.g. "jp-1.ovpn"
    val host: String,        // VPN Gate hostname
    val ip: String,          // server IP (ping target)
    val proto: String,       // "udp" or "tcp"
    val port: Int,           // OpenVPN port from the config's remote line
    var pingMs: Long = -1    // measured latency, -1 = not measured
)

object ServerRepository {
    /** Loads the embedded server list from assets/servers/servers.json. */
    fun load(context: android.content.Context): List<Server> {
        val json = context.assets.open("servers/servers.json")
            .bufferedReader().use { it.readText() }
        val arr = JSONArray(json)
        val out = ArrayList<Server>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out += Server(
                id = o.getString("id"),
                country = o.getString("country"),
                short = o.getString("short"),
                flag = o.getString("flag"),
                file = o.getString("file"),
                host = o.getString("host"),
                ip = o.getString("ip"),
                proto = o.getString("proto"),
                port = o.optInt("port", if (o.getString("proto") == "udp") 1194 else 443)
            )
        }
        return out
    }
}
