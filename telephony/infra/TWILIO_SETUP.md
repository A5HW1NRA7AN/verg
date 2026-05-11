# Twilio Trunk & Webhook Setup Guide

This guide walks you through connecting your Twilio SIP Trunk to your new Asterisk/FreePBX EC2 instance.

## 1. Twilio Console Setup

1. **Log in to Twilio** and navigate to **Voice** -> **Manage** -> **SIP Trunks**.
2. **Create a new SIP Trunk** and name it something like `VERG-PBX`.
3. In your new trunk, go to **Termination**:
   - Under **Termination SIP URI**, enter a unique domain (e.g., `verg-telephony-xyz.pstn.twilio.com`).
   - Under **Access Control Lists (ACL)**, create a new IP Access Control List. Add your FreePBX EC2 **Public IP** (found in your Terraform outputs) to this list. This ensures only your server can send outbound calls (if you ever need to).
   - Under **Credential Lists**, you can leave this empty since we are using IP authentication.
4. Go to **Origination**:
   - Add a new **Origination URI**. 
   - Enter `sip:<your-ec2-public-ip>`
   - This tells Twilio: *When someone calls my Twilio number, forward the call to my FreePBX server via SIP over UDP 5060.*
5. Go to **Numbers**:
   - Assign your purchased Twilio phone number to this SIP Trunk.

## 2. FreePBX Trunk Configuration

Once your EC2 instance is running and FreePBX is installed, we need to tell FreePBX to accept calls from Twilio.

1. Log into your FreePBX Web Admin UI.
2. Go to **Connectivity** -> **Trunks**.
3. Click **Add Trunk** -> **Add SIP (chan_pjsip) Trunk**.

**General Tab:**
- Trunk Name: `Twilio`

**pjsip Settings -> General Tab:**
- Authentication: `None`
- Registration: `None`
- SIP Server: `<your-twilio-domain>.pstn.twilio.com`
- SIP Server Port: `5060`
- Context: `from-twilio-missed-call`  *(This is critical! It links the trunk to our custom dialplan)*

**pjsip Settings -> Advanced Tab:**
- Match (Permit): This is the list of IPs that Twilio will send calls from. Paste the following:
  ```
  54.172.60.0/30, 54.244.51.0/30, 54.171.127.192/30, 35.156.191.128/30, 54.65.63.192/30, 54.169.127.128/30, 54.252.254.64/30, 177.71.206.192/30
  ```

Click **Submit** and then **Apply Config**.

## 3. Applying the dialplan (call flow)

1. SSH into your FreePBX EC2 instance:
   ```bash
   ssh -i asterisk-key.pem admin@<ec2-ip>
   ```
2. Open the custom dialplan file:
   ```bash
   nano /etc/asterisk/extensions_custom.conf
   ```
3. Paste the contents of `telephony/extensions_custom.conf` (from this repository) into the file.
4. Ensure the **greeting** file exists at `custom/atc-greeting` in Asterisk sounds (upload via FreePBX **Admin → File Store** or place under `/var/lib/asterisk/sounds/custom/`).
5. Save the file (`Ctrl+X`, then `Y`, then `Enter`).
6. Reload the dialplan:
   ```bash
   asterisk -rx "dialplan reload"
   ```

## 4. telephony-service: AMI ingest (recommended)

Leads are created when `telephony-service` receives **AMI Hangup** events for context `from-twilio-missed-call`. Configure `telephony-service` to connect to the PBX manager port and enable the client:

- Follow **[AMI_SETUP.md](AMI_SETUP.md)** (`telephony.ami.*`, `telephony.lead-context-allowlist`, optional `telephony.lead-registry.url`).

**POC note:** Twilio free trial / trial accounts are fine for SIP + DID testing; for production you may move DIDs and trunks to another carrier without changing the AMI-based Java pipeline — see **[CARRIER_PORTABILITY.md](CARRIER_PORTABILITY.md)**.

## 5. Test the system

1. Start VERG with AMI enabled and correct `telephony.ami.*` credentials.
2. Call your Twilio number from a mobile phone.
3. You should hear ringing, then the greeting, then hangup.
4. In VERG logs, confirm AMI connection and ingest; in Postgres, check table `telephony_call_lead_ingest_log`.
5. For live Asterisk CLI:
   ```bash
   asterisk -rvvv
   ```
