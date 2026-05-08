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

## 3. Applying the Dialplan Webhook

1. SSH into your FreePBX EC2 instance:
   ```bash
   ssh -i asterisk-key.pem admin@<ec2-ip>
   ```
2. Open the custom dialplan file:
   ```bash
   nano /etc/asterisk/extensions_custom.conf
   ```
3. Paste the contents of `telephony/extensions_custom.conf` (from this repository) into the file.
4. Make sure to update the `VERG_URL` line to point to your actual VERG server URL!
5. Save the file (`Ctrl+X`, then `Y`, then `Enter`).
6. Reload the dialplan into Asterisk memory:
   ```bash
   asterisk -rx "dialplan reload"
   ```

## 4. Test the System!

1. Start your VERG server locally.
2. Call your Twilio phone number from your cell phone.
3. You should hear it ring for 2 seconds, then abruptly hang up.
4. Check your VERG server logs—you should see the JSON payload arrive!
5. To see the Asterisk logs live as the call happens, run:
   ```bash
   asterisk -rvvv
   ```
