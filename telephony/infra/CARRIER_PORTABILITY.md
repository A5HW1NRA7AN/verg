# Twilio POC vs production telecom carrier

## What the POC uses today

- **Twilio** (including free trial where applicable): **DID** + **SIP trunk** terminating on **FreePBX** on AWS, as described in [TWILIO_SETUP.md](TWILIO_SETUP.md).
- **telephony-service** connects to the same PBX over **AMI** regardless of who sells the SIP trunk ([AMI_SETUP.md](AMI_SETUP.md)).

## What stays the same when you move to production

- **FreePBX / Asterisk** as the PBX.
- **AMI event pipeline** in `telephony-service` (Hangup → log row → optional HTTP to lead registry).
- **Lead JSON shape** and **Postgres ingest log** semantics.
- **Dialplan contexts** you use for campaigns (e.g. `from-twilio-missed-call` can be renamed once in both FreePBX and `telephony.lead-context-allowlist`).

## What changes with a new carrier

| Area | Change |
|------|--------|
| SIP trunk | New provider **SIP domain**, **auth** (IP vs digest), **codec** policy, **allowed signalling IPs** on your EC2 security group. |
| Origination | Inbound calls must be routed to your PBX **public IP / SBC** the same way Twilio “Origination URI” does today. |
| DID | Numbers are **ported or newly assigned** at the carrier; update inbound routes in FreePBX. |
| ACLs | Replace Twilio’s published SIP ranges with the **new carrier’s** IP ranges on the trunk / firewall. |

## What should not be hard-coded in Java

- Avoid “Twilio” checks in `telephony-service`; use **AMI fields** (`Context`, `Channel`, `CallerIDNum`, `Exten`, `Uniqueid`, `Linkedid`, etc.) and **configuration** (`telephony.lead-context-allowlist`, trunk names if you later filter on `Channel`).

Keeping provider-specific steps in **this `telephony/` folder** (Terraform + markdown) preserves a clean boundary for operations.
