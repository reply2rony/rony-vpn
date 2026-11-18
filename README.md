# Rony VPN

A free, no-account Android VPN for Rony's own phone. One tap to connect,
pick a country or tap **Fastest Server** (the app ping-tests the listed
servers and connects to the lowest-latency one). Built on the trusted
open-source **ics-openvpn** core (OpenVPN) — no custom crypto, no custom
tunnel, no account, no ads, no tracking.

## Install (sideload)

1. On the phone: **Settings → Security → Allow install from unknown sources**
   (or allow it just for the browser / file manager you open the APK with).
2. Copy `RonyVPN.apk` to the phone and tap it to install.
3. Open the app, pick a country (or **Fastest Server**), tap **Connect**,
   and accept the Android VPN permission prompt ("Rony VPN wants to set up
   a VPN connection").
4. To disconnect, tap **Disconnect** in the app.

Requires Android 7.0 (API 24) or newer.

## Servers

See [SERVER-LIST.md](SERVER-LIST.md). The servers are free public
**VPN Gate** servers (University of Tsukuba project), fetched live from
`http://www.vpngate.net/api/iphone/` on 2026-11-18 and embedded in the app.
No username or password is needed for any of them.

**Honest limits:**
- **Free volunteer servers.** Speeds vary with the server and the time of
  day; a free server can be slow or busy. "Fastest Server" means the
  lowest ping among the free servers listed in the app — not the fastest
  VPN in the world.
- **Servers rotate.** VPN Gate volunteers come and go; a server that works
  today can disappear tomorrow. If a server stops connecting, try another
  country or tap Fastest Server again. A future build can refresh the
  list (`servers/select_servers.py` + `servers/embed_servers.py`).
- **Country coverage right now: Japan and South Korea only.** The VPN Gate
  feed was checked 4 times on 2026-11-17/18 and listed no servers in the
  US, Canada, Australia, or Singapore at those times. When VPN Gate lists
  them again, they can be added with the same refresh scripts — no code
  change needed (the app already supports all six countries).

## Security — what this app does and does not do

**What the app does:**
- **Collects nothing.** No analytics SDK, no ads SDK, no tracking, no
  account or sign-up, no browsing logs stored anywhere — nothing is sent
  to any server of ours (there is no "ours" server).
- **Kill switch (app-level).** The VPN is configured so no app may bypass
  it and the tunnel stays up across reconnects (`allowBypass=false`,
  persist-tun). If the tunnel drops, traffic is blocked until it
  reconnects or you disconnect. For a **full system-level kill switch**,
  also turn on Android's own setting: in the app tap **Kill-switch
  settings** → tap Rony VPN → enable **Always-on VPN** and **Block
  connections without VPN**. That is an Android setting the app cannot
  turn on by itself, and it is the only way to guarantee zero leaks.
- **No DNS leak.** All DNS is routed through the tunnel to 1.1.1.1
  (Cloudflare) and 9.9.9.9 (Quad9). The VPN Gate configs push no DNS of
  their own, so the app sets these explicitly.
- **Trusted core, as-is.** The VPN is the unmodified open-source
  ics-openvpn implementation (github.com/schwabe/ics-openvpn, GPL-2.0),
  vendored in `source/openvpn/`. No custom crypto, no custom tunnel.
- **No credentials stored.** The embedded configs need no username or
  password. Nothing sensitive is stored in SharedPreferences; no tokens,
  no keys.
- **Minimal permissions.** Only INTERNET, ACCESS_NETWORK_STATE (needed by
  the OpenVPN core to notice network changes — a normal permission, no
  prompt), FOREGROUND_SERVICE and FOREGROUND_SERVICE_SPECIAL_USE. No
  location, contacts, storage, or phone-state permissions — the app never
  asks for them.

**What the app cannot fix (be honest about this):**
- The VPN Gate servers are **volunteer-run**. The operator of the server
  you connect to can see connection metadata — which destination IPs you
  connect to, and DNS queries if they are not encrypted. That is a
  property of free public servers, not something this app can change.
- **HTTPS traffic stays encrypted end-to-end** — a server operator cannot
  read the contents of your HTTPS browsing. Plain unencrypted (HTTP)
  traffic could be seen by the server operator, so prefer HTTPS sites
  and apps, as always.
- A VPN hides your traffic from your local network and ISP, but it does
  **not** make you anonymous: the VPN server itself, and the websites you
  log into, still know who you are.

## Build from source

Full source is in `source/` (Kotlin app + vendored ics-openvpn module).

**Status (honest): the APK was NOT built on this VM.** The source is
complete and the SDK is installed, but the Gradle build cannot run here —
see "Build blocker" below. The APK still needs one build on a normal
machine (or in Android Studio), then copy it here as `RonyVPN.apk`.

**To build (normal machine / Android Studio):**
- Needs JDK 17, Android SDK platform 36, build-tools 37.0.0,
  NDK 30.0.14904198, CMake 3.22.1.
- Open `source/` in Android Studio (it offers to install the SDK pieces),
  or from a terminal run `./build.sh` (in this folder) — it runs
  `./gradlew :app:assembleDebug` in `source/`.
- The APK lands at `source/app/build/outputs/apk/debug/app-debug.apk`.
- Verify it with `aapt dump badging` and check the merged manifest
  permissions are only INTERNET, ACCESS_NETWORK_STATE, FOREGROUND_SERVICE
  and FOREGROUND_SERVICE_SPECIAL_USE.
- Or build free on GitHub Actions: every push to `main` runs
  `.github/workflows/build-apk.yml` (JDK 17, SDK platform 36,
  build-tools 37.0.0, NDK 30.0.14904198, CMake 3.22.1) and uploads the
  APK as the `app-debug` workflow artifact (download it from the run
  page; requires a GitHub login). The workflow installs Gradle 9.7.1 and
  regenerates the Gradle wrapper, because `gradle-wrapper.jar` is a
  binary and is not stored in this repo.
- Files intentionally NOT in this repo (the build does not need them):
  the vendored ics-openvpn launcher PNGs (`openvpn/src/main/res/mipmap-*`,
  unreferenced — the app supplies its own vector icon), prebuilt lzo
  `.o`/`.obj` files (rebuilt from source), lzo autoconf `shtool`, test
  certificates (`client.p12`, `insta.ca.crt`), and machine-specific
  `local.properties`.

**Build blocker on this VM (2026-11-18):** Gradle's daemon IPC fails.
The Gradle client cannot talk to its own daemon over loopback: every
invocation ends with "Could not dispatch a message to the daemon" /
"Could not write '/127.0.0.1:PORT'" (Broken pipe) / "Could not receive a
message from the daemon". The daemon itself starts fine and listens, but
never accepts the client's connection. Ruled out: plain loopback TCP and
Java sockets (blocking and NIO) both work on this VM; the failure is
specific to Gradle's client↔daemon socket on this sandbox. Tried and
failed: single-use daemon, persistent daemon, heap 512 MB + SerialGC,
`-Djava.net.preferIPv4Stack=true`, running the Gradle 9.7.1 binary
directly (the wrapper's downloader also fails — Java's HttpURLConnection
cannot do TLS through this VM's egress proxy). No Docker/Podman is
available to isolate the build. The SDK was installed manually instead
(platform 36, build-tools 37.0.0, NDK 30, CMake 3.22.1, licenses accepted;
see `download-sdk.sh` and `setup-sdk.sh`), and Gradle 9.7.1 is at
`~/gradle-9.7.1`, so a retry on this VM only needs the daemon IPC fixed.
