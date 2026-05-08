# Asterisk & FreePBX Post-Installation Guide

Because the `main.tf` uses an AWS `user_data` script, the installation of FreePBX happens automatically in the background right after the EC2 instance boots.

## 1. Verify Installation Progress
Once `terraform apply` finishes, it will output an SSH command to access your server.
1. Run the provided SSH command (e.g., `ssh -i asterisk-key.pem admin@<ip>`).
2. Run this command to watch the installation logs in real-time:
   ```bash
   tail -f /var/log/cloud-init-output.log
   ```
3. When you see `Installation Complete!` or the log finishes, you can exit the SSH session.

## 2. FreePBX Initial Setup
1. Go to the `freepbx_url` output from Terraform in your web browser.
2. Create an admin username, password, and enter your email address.
3. Click **Setup System**.

## 3. Configure NAT
Since the server is running in an AWS VPC, it is behind a NAT.
1. In the FreePBX GUI, go to **Settings** -> **Asterisk SIP Settings**.
2. Under the **General SIP Settings** tab, click **Detect Network Settings** (this automatically fills in your AWS public IP).
3. Ensure **Local Networks** is set correctly (e.g., `172.31.0.0/16` for the default AWS VPC).
4. Click **Submit** and then click the red **Apply Config** button at the top right.

## 4. Configure Twilio PJSIP Trunk
1. Go to **Connectivity** -> **Trunks**.
2. Click **Add Trunk** -> **Add SIP (chan_pjsip) Trunk**.
3. **General Tab:**
   - Trunk Name: `Twilio`
4. **pjsip Settings -> General Tab:**
   - Authentication: `None`
   - Registration: `None`
   - SIP Server: `<your-twilio-sip-domain>.pstn.twilio.com`
   - SIP Server Port: `5060`
   - Context: `from-twilio-missed-call`
5. **pjsip Settings -> Advanced Tab:**
   - Match (Permit): Paste Twilio's IP ranges: `54.172.60.0/30, 54.244.51.0/30, 54.171.127.192/30, 35.156.191.128/30, 54.65.63.192/30, 54.169.127.128/30, 54.252.254.64/30, 177.71.206.192/30`
6. Click **Submit** and **Apply Config**.

## 5. Install Custom Dialplan
1. Open the `extensions_custom.conf` file provided in this repository.
2. Update the `http://verg-backend.local:8080` placeholder to point to your VERG server (use Ngrok or Tailscale if VERG is running on your local machine).
3. SSH into your Asterisk server.
4. Edit the custom extensions file:
   ```bash
   nano /etc/asterisk/extensions_custom.conf
   ```
5. Paste the content of `extensions_custom.conf` and save the file (`Ctrl+X`, `Y`, `Enter`).
6. Reload the Asterisk dialplan:
   ```bash
   asterisk -rx "dialplan reload"
   ```

You are now ready to make a test call to your Twilio number and see the JSON payload appear in your VERG backend!
