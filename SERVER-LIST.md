# Rony VPN — embedded server list

Fetched live from the VPN Gate public API
(`http://www.vpngate.net/api/iphone/`) — 4 snapshots merged
(2026-11-17 23:30, 2026-11-18 00:00, 00:30, 00:55 Dhaka).
Selection: 3 servers per country, preferring OpenVPN UDP configs with no
username/password, highest score, lowest ping. All configs are anonymous
(no auth-user-pass) and carry inline CA/cert/key blocks (VPN Gate's public
client certificate — not a secret).

## Japan (JP) — 3 servers

| Server | Hostname | IP | Protocol | Port | Score | Ping |
|---|---|---|---|---|---|---|
| jp-1 | vpn724833364 | 118.240.250.95 | UDP | 1343 | 1,255,892 | 2 ms |
| jp-2 | vpn898388129 | 39.111.215.189 | UDP | 1195 | 784,170 | 2 ms |
| jp-3 | vpn270991769 | 131.147.158.37 | UDP | 1831 | 759,221 | 2 ms |

## South Korea (KR) — 3 servers

| Server | Hostname | IP | Protocol | Port | Score | Ping |
|---|---|---|---|---|---|---|
| kr-1 | vpn737724616 | 175.199.193.131 | UDP | 1419 | 1,220,294 | 28 ms |
| kr-2 | vpn446015046 | 59.10.232.165 | TCP | 1755 | 1,488,325 | 30 ms |
| kr-3 | vpn532221228 | 110.15.34.238 | TCP | 1930 | 1,468,947 | 28 ms |

## Not available right now

**US, Canada, Australia, Singapore: 0 servers** in any of the 4 snapshots
(97 unique servers seen: JP 45, KR 30–31, RU 9–10, IN 3, TH 3, VN 2–4,
RO 1, MY 1, TR 1, DE 0–1). VPN Gate is volunteer-run and rotates — these
countries had no servers listed at fetch time. The app already supports
all six countries; when VPN Gate lists US/CA/AU/SG again, re-run
`servers/select_servers.py` + `servers/embed_servers.py` and rebuild.

Date fetched: **2026-11-18** (snapshots 2026-11-17 23:30 → 2026-11-18 00:55 Dhaka).
