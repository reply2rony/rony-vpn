package com.rony.vpn

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.blinkt.openvpn.core.ConnectionStatus
import de.blinkt.openvpn.core.VpnStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Rony VPN — single screen.
 * Country list (flags + live ping) • "Fastest Server" • one-tap Connect.
 * Shows connection state, server, public IP + location, duration, data.
 * No account, no logs, no analytics, no ads — see README.md "Security".
 */
class MainActivity : AppCompatActivity() {

    private lateinit var vpn: VpnController
    private var servers: List<Server> = emptyList()
    private var selected: Server? = null
    private var adapter: ServerAdapter? = null
    private var connectedSince = 0L
    private var pendingServer: Server? = null

    private lateinit var statusText: TextView
    private lateinit var serverText: TextView
    private lateinit var ipText: TextView
    private lateinit var durationText: TextView
    private lateinit var dataText: TextView
    private lateinit var connectButton: Button
    private lateinit var fastestButton: Button

    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (connectedSince > 0) {
                val s = (System.currentTimeMillis() - connectedSince) / 1000
                durationText.text = "Connected: %02d:%02d".format(s / 60, s % 60)
                timerHandler.postDelayed(this, 1000)
            }
        }
    }

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pendingServer?.let { connectNow(it) }
        } else {
            Toast.makeText(this, "VPN permission denied", Toast.LENGTH_SHORT).show()
        }
        pendingServer = null
    }

    private val stateListener = VpnStatus.StateListener { _, _, _, level, _ ->
        runOnUiThread { onStateChanged(level) }
    }

    private val byteListener = VpnStatus.ByteCountListener { inBytes, outBytes, _, _ ->
        runOnUiThread { dataText.text = "↓ ${fmtBytes(inBytes)}   ↑ ${fmtBytes(outBytes)}" }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        vpn = VpnController(this)

        statusText = findViewById(R.id.status_text)
        serverText = findViewById(R.id.server_text)
        ipText = findViewById(R.id.ip_text)
        durationText = findViewById(R.id.duration_text)
        dataText = findViewById(R.id.data_text)
        connectButton = findViewById(R.id.connect_button)
        fastestButton = findViewById(R.id.fastest_button)
        val list: RecyclerView = findViewById(R.id.server_list)

        servers = try {
            ServerRepository.load(this)
        } catch (e: Exception) {
            emptyList()
        }
        adapter = ServerAdapter(servers, null) { s ->
            selected = s
            adapter?.update(servers, s.id)
            serverText.text = "Server: ${s.country} • ${s.ip} (${s.proto.uppercase()})"
        }
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        connectButton.setOnClickListener { onConnectClicked() }
        fastestButton.setOnClickListener { onFastestClicked() }
        findViewById<Button>(R.id.killswitch_settings).setOnClickListener {
            startActivity(Intent(Settings.ACTION_VPN_SETTINGS))
        }

        VpnStatus.addStateListener(stateListener)
        VpnStatus.addByteCountListener(byteListener)
        onStateChanged(if (vpn.isActive()) ConnectionStatus.LEVEL_CONNECTED
            else ConnectionStatus.LEVEL_NOTCONNECTED)

        // Measure latency to every server in the background.
        lifecycleScope.launch { measurePings() }
    }

    override fun onDestroy() {
        VpnStatus.removeStateListener(stateListener)
        VpnStatus.removeByteCountListener(byteListener)
        timerHandler.removeCallbacks(timerRunnable)
        super.onDestroy()
    }

    private suspend fun measurePings() {
        val measured = PingTester.pingAll(servers)
        servers = measured
        if (selected == null) {
            selected = servers.filter { it.pingMs >= 0 }.minByOrNull { it.pingMs }
                ?: servers.firstOrNull()
        }
        withContext(Dispatchers.Main) {
            adapter?.update(servers, selected?.id)
            selected?.let {
                serverText.text = "Server: ${it.country} • ${it.ip} (${it.proto.uppercase()})"
            }
        }
    }

    private fun onConnectClicked() {
        if (vpn.isActive()) {
            vpn.disconnect()
            return
        }
        val server = selected ?: servers.firstOrNull()
        if (server == null) {
            Toast.makeText(this, "No servers available", Toast.LENGTH_SHORT).show()
            return
        }
        requestConnect(server)
    }

    private fun onFastestClicked() {
        val fastest = servers.filter { it.pingMs >= 0 }.minByOrNull { it.pingMs }
        if (fastest == null) {
            Toast.makeText(this, "Still measuring — try again in a moment", Toast.LENGTH_SHORT).show()
            return
        }
        selected = fastest
        adapter?.update(servers, fastest.id)
        serverText.text = "Server: ${fastest.country} • ${fastest.ip} (${fastest.proto.uppercase()})"
        if (!vpn.isActive()) requestConnect(fastest)
    }

    private fun requestConnect(server: Server) {
        val intent = vpn.prepareIntent()
        if (intent != null) {
            pendingServer = server
            vpnPermissionLauncher.launch(intent)
        } else {
            connectNow(server)
        }
    }

    private fun connectNow(server: Server) {
        selected = server
        adapter?.update(servers, server.id)
        connectButton.isEnabled = false
        lifecycleScope.launch(Dispatchers.IO) {
            val result = vpn.connect(server)
            withContext(Dispatchers.Main) {
                connectButton.isEnabled = true
                result.onFailure {
                    Toast.makeText(
                        this@MainActivity,
                        "Connect failed: ${it.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun onStateChanged(level: ConnectionStatus) {
        when (level) {
            ConnectionStatus.LEVEL_CONNECTED -> {
                statusText.text = "● Connected"
                connectButton.text = "Disconnect"
                connectButton.isEnabled = true
                fastestButton.isEnabled = false
                if (connectedSince == 0L) {
                    connectedSince = System.currentTimeMillis()
                    timerHandler.post(timerRunnable)
                    lifecycleScope.launch { refreshPublicIp() }
                }
            }
            ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET,
            ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED,
            ConnectionStatus.LEVEL_START -> {
                statusText.text = "Connecting…"
                connectButton.text = "Connecting…"
                connectButton.isEnabled = false
                fastestButton.isEnabled = false
            }
            else -> {
                statusText.text = "○ Disconnected"
                connectButton.text = "Connect"
                connectButton.isEnabled = true
                fastestButton.isEnabled = true
                connectedSince = 0L
                timerHandler.removeCallbacks(timerRunnable)
                durationText.text = "Connected: 00:00"
                ipText.text = "IP: —"
                dataText.text = "↓ —   ↑ —"
                if (level == ConnectionStatus.LEVEL_AUTH_FAILED) {
                    Toast.makeText(this, "Server rejected the connection", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /** Shows the public IP + country as seen through the tunnel. */
    private suspend fun refreshPublicIp() {
        val info = withContext(Dispatchers.IO) {
            try {
                val conn = URL("https://ip-api.com/json/?fields=status,country,query")
                    .openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                val text = conn.inputStream.bufferedReader().readText()
                val obj = JSONObject(text)
                if (obj.optString("status") == "success") {
                    obj.getString("query") to obj.getString("country")
                } else null
            } catch (e: Exception) {
                null
            }
        }
        if (vpn.isActive()) {
            ipText.text = if (info != null) "IP: ${info.first} • ${info.second}"
                else "IP: connected (lookup failed)"
        }
    }

    private fun fmtBytes(b: Long): String = when {
        b < 1024 -> "$b B"
        b < 1024 * 1024 -> "%.1f KB".format(b / 1024.0)
        b < 1024 * 1024 * 1024 -> "%.1f MB".format(b / 1024.0 / 1024.0)
        else -> "%.2f GB".format(b / 1024.0 / 1024.0 / 1024.0)
    }
}
