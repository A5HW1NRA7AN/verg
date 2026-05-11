# AMI integration (telephony-service ↔ Asterisk / FreePBX)

`telephony-service` can consume **Asterisk Manager Interface (AMI)** events over a long-lived TCP connection and turn **Hangup** events in configured dialplan contexts into **lead rows** (Postgres) plus optional **HTTP POST** to your lead registry.

## Network path

- **Direction:** `telephony-service` (or any host running it) opens **outbound TCP** to the PBX on the manager port (default **5038**).
- **Firewall:** On the EC2 security group (or host firewall), allow **`VERG_source_IP → PBX:5038`** only. Do not expose 5038 to the public internet without VPN or strict ACLs.

## FreePBX: enable Manager and create a user

1. In FreePBX UI: **Settings → Asterisk Manager Users** (or **Admin → Asterisk SIP Settings → Manager** depending on version).
2. Create a dedicated user (e.g. `verg_ami`) with a strong secret.
3. **Permit** only the IP address(es) from which VERG will connect (your app server or VPN egress).
4. Grant **read** (and optionally minimal **write** only if you later need originate from VERG). For missed-call ingest, **read** is enough.

## telephony-service configuration (`application.properties` or env)

| Property | Description |
|----------|-------------|
| `telephony.ami.enabled` | `true` to start the AMI client thread (default `false` for local/CI). |
| `telephony.ami.host` | PBX hostname or IP reachable from VERG. |
| `telephony.ami.port` | Usually `5038`. |
| `telephony.ami.username` / `telephony.ami.secret` | Manager credentials. |
| `telephony.ami.event-mask` | Comma-separated AMI mask (default `call,system,user`). |
| `telephony.lead-context-allowlist` | Exact **Context** values for which a **Hangup** creates a lead (e.g. `from-twilio-missed-call`). |
| `telephony.lead-registry.url` | HTTPS POST target for the normalized JSON; **empty** skips outbound and marks the row `SKIPPED_NO_REGISTRY_URL`. |
| `telephony.lead-registry.api-key-header` / `api-key-value` | Optional header for the lead API. |

## Behaviour

1. On **Hangup** in an allowlisted **Context**, `telephony-service` inserts `telephony_call_lead_ingest_log` with status `RECEIVED` (idempotent on **`Linkedid`** if present, else **`Uniqueid`**).
2. After commit, an **async** job POSTs `normalized_lead_payload` to `telephony.lead-registry.url` when set.
3. A **scheduled sweeper** retries rows stuck in `RECEIVED` / `SENDING` older than `telephony.dispatch.stale-after-seconds`.

## Security hardening

- Prefer **VPN or SSH tunnel** from `telephony-service` to PBX so 5038 is never on the public Internet.
- If you must use the open Internet, combine **TLS tunnel (stunnel)** or **IP allowlisting** with a **dedicated low-privilege** manager user.

## Operational notes

- The client sends **Ping** on read timeout to keep the session healthy.
- On disconnect, VERG **reconnects** with exponential backoff (`telephony.ami.reconnect-initial-ms` … `reconnect-max-ms`).
