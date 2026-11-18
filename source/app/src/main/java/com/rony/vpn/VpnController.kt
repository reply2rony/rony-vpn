package com.rony.vpn

import android.content.Context
import android.content.Intent
import android.net.VpnService
import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.activities.DisconnectVPN
import de.blinkt.openvpn.core.ConfigParser
import de.blinkt.openvpn.core.OpenVPNService
import de.blinkt.openvpn.core.ProfileManager
import de.blinkt.openvpn.core.VPNLaunchHelper
import de.blinkt.openvpn.core.VpnStatus
import java.io.StringReader

/**
 * Connects / disconnects the ics-openvpn core using the embedded VPN Gate
 * configs. The VPN core itself is the trusted open-source ics-openvpn
 * implementation — this class only feeds it a parsed config and applies the
 * security hardening. No custom crypto, no custom tunnel, no credentials.
 */
class VpnController(private val context: Context) {

    companion object {
        // Public DNS resolvers used inside the tunnel. The VPN Gate configs
        // push no DNS of their own, so we set them explicitly — otherwise
        // DNS could fall back to the physical network (DNS leak).
        const val DNS1 = "1.1.1.1" // Cloudflare
        const val DNS2 = "9.9.9.9" // Quad9
    }

    fun isActive(): Boolean = VpnStatus.isVPNActive()

    /** Builds a hardened profile from the embedded .ovpn and starts the VPN.
     *  Call only after the VpnService permission has been granted
     *  (see [prepareIntent]). Runs the parse on the calling thread — call
     *  from a background thread. */
    fun connect(server: Server): Result<String> {
        return try {
            val configText = context.assets.open("servers/${server.file}")
                .bufferedReader().use { it.readText() }
            val parser = ConfigParser()
            parser.parseConfig(StringReader(configText))
            val profile = parser.convertProfile()
            profile.mName = "RonyVPN ${server.country} (${server.host})"

            // --- Hardening (security requirements) ---
            profile.mUseDefaultRoute = true     // route ALL traffic via tunnel
            profile.mUseDefaultRoutev6 = true
            profile.mOverrideDNS = true        // no DNS leak: tunnel DNS only
            profile.mDNS1 = DNS1
            profile.mDNS2 = DNS2
            profile.mAllowLocalLAN = false     // no local-network bypass
            profile.mAllowAppVpnBypass = false // no app may bypass the VPN
            profile.mPersistTun = true         // keep tun up across reconnects

            // Replace any older profile with the same name, then persist it
            // (the service loads the profile from disk by UUID).
            val pm = ProfileManager.getInstance(context)
            pm.getProfileByName(profile.mName)?.let { pm.removeProfile(context, it) }
            pm.addProfile(profile)
            ProfileManager.saveProfile(context, profile)

            VPNLaunchHelper.startOpenVpn(profile, context, "RonyVPN", true)
            Result.success(profile.mName)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun disconnect() {
        val intent = Intent(context, DisconnectVPN::class.java).apply {
            action = OpenVPNService.DISCONNECT_VPN
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /** Intent to request the VPN permission, or null if already granted. */
    fun prepareIntent(): Intent? = VpnService.prepare(context)
}
