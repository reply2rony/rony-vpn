// Rony VPN — root build file.
// The :openvpn module is the ics-openvpn core (github.com/schwabe/ics-openvpn,
// GPL-2.0) vendored as a library module, skeleton + ovpn2 flavors only.
// See README.md in ~/workspace/rony-vpn for licensing and provenance.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
}
