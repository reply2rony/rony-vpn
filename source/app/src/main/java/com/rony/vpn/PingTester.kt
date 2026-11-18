package com.rony.vpn

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Measures latency to a server. Tries the system `ping` binary first
 * (works without root on Android), falls back to a TCP connect timing
 * against the server's OpenVPN port. Returns -1 when unreachable.
 * No data leaves the device except the probe packets themselves.
 */
object PingTester {
    fun ping(server: Server, timeoutMs: Int = 2000): Long {
        val viaPing = pingBinary(server.ip, timeoutMs)
        if (viaPing >= 0) return viaPing
        return tcpConnect(server)
    }

    private fun pingBinary(ip: String, timeoutMs: Int): Long {
        return try {
            val seconds = (timeoutMs / 1000).coerceAtLeast(1)
            val proc = Runtime.getRuntime()
                .exec(arrayOf("/system/bin/ping", "-c", "2", "-W", seconds.toString(), ip))
            val out = proc.inputStream.bufferedReader().readText()
            proc.waitFor()
            // line: rtt min/avg/max/mdev = 12.345/13.210/14.100/0.800 ms
            val rtt = out.lines().firstOrNull { it.contains("rtt min/avg/max") }
                ?: return -1
            val avg = rtt.substringAfter("=").trim().split("/").getOrNull(1)
                ?.trim()?.toDoubleOrNull() ?: return -1
            avg.toLong()
        } catch (e: Exception) {
            -1
        }
    }

    private fun tcpConnect(server: Server): Long {
        // Probe the server's actual OpenVPN port (from its config remote line).
        val port = server.port
        return try {
            val start = System.currentTimeMillis()
            Socket().use { s ->
                s.connect(InetSocketAddress(server.ip, port), 2000)
            }
            System.currentTimeMillis() - start
        } catch (e: Exception) {
            -1
        }
    }

    /** Pings all servers in parallel and returns them with pingMs filled in. */
    suspend fun pingAll(servers: List<Server>): List<Server> = coroutineScope {
        servers.map { s ->
            async(Dispatchers.IO) { s.copy(pingMs = ping(s)) }
        }.awaitAll()
    }
}
